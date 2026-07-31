/*
 *
 *  Copyright (c) Lightstreamer Srl
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.lightstreamer.examples.roomball_demo.adapters;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lightstreamer.adapter.common.InitException;
import com.lightstreamer.adapter.common.Mode;
import com.lightstreamer.adapter.data.DataAdapter;
import com.lightstreamer.adapter.data.ItemListener;
import com.lightstreamer.adapter.data.ItemManager;
import com.lightstreamer.adapter.data.SubscriptionException;

public class RoomBallAdapter implements DataAdapter, Publisher {

    static final String ROOM_DEMO_LOGGER_NAME = "LS_demos_Logger.RoomBallDemo";
    
    private static final String CFG_PARAM_FRAME_RATE = "frameRate";
    private static final String CFG_PARAM_STEPS_PER_FRAME = "stepsPerFrame";

    private static final String ITEM_NAME_PLAYERS_LIST = "Players_list";
    private static final String ITEM_NAME_PREFIX_BAND = "My_Band_";

    private Logger logger;

    /**
     * The active PlayersListItemManager for the "Players_list" item.
     */
    private volatile PlayersListItemManager playerListManager = null;

    /**
     * Map of active BandwidthItemManagers, indexed by item name.
     */
    private final ConcurrentHashMap<String, BandwidthItemManager> bandManagers =
            new ConcurrentHashMap<>();

    /**
     * The room where the players and the ball move
     */
    private Room room;

    private Map<String, String> initParams;

    // Public Methods Implementing DataAdapter Methods -------------------------

    @Override
    public void init(Map<String, String> params, File configDir) throws InitException {

        try {
            logger = LogManager.getLogger(ROOM_DEMO_LOGGER_NAME);
        } catch (Exception e) {
            System.out.println("Loggers failed to load: " + e);
        }

        initParams = params;
        
        logger.debug("RoomAdapter start!");
    }

    @Override
    public CompletionStage<ItemManager> getItemManager(String itemName) throws SubscriptionException {
        if (itemName.equals(ITEM_NAME_PLAYERS_LIST)) {
            return CompletableFuture.completedFuture(new PlayersListItemManager(itemName));
        } else if (itemName.startsWith(ITEM_NAME_PREFIX_BAND)) {
            return CompletableFuture.completedFuture(new BandwidthItemManager(itemName));
        }
        CompletableFuture<ItemManager> f = new CompletableFuture<>();
        f.completeExceptionally(new SubscriptionException("No such item: " + itemName));
        return f;
    }

    @Override
    public void shutdown() {
    }

    // Public Methods ---------------------------------------------------------

    public void setRoom(Box2DRoom box2dRoom) {
        box2dRoom.setPublisher(this);

        if (initParams.containsKey(CFG_PARAM_FRAME_RATE)) {
            logger.debug("frameRate configured: " + initParams.get(CFG_PARAM_FRAME_RATE));
            box2dRoom.setFrameRate(Integer.parseInt(initParams.get(CFG_PARAM_FRAME_RATE)));
        }

        if (initParams.containsKey(CFG_PARAM_STEPS_PER_FRAME)) {
            logger.debug("Steps per Frame configured: " + initParams.get(CFG_PARAM_STEPS_PER_FRAME));
            box2dRoom.setStepsPerFrame(Integer.parseInt(initParams.get(CFG_PARAM_STEPS_PER_FRAME)));
        }

        room = box2dRoom;
    }

    // Inner ItemManager Classes -----------------------------------------------

    private class PlayersListItemManager implements ItemManager {

        private final String itemName;
        private ItemListener listener;

        PlayersListItemManager(String itemName) {
            this.itemName = itemName;
        }

        @Override
        public Mode getSupportedMode() {
            return Mode.COMMAND;
        }

        @Override
        public boolean isSnapshotAvailable() {
            return true;
        }

        @Override
        public CompletionStage<Void> subscribe(ItemListener listener) {
            logger.debug("Subscribe request for '" + ITEM_NAME_PLAYERS_LIST + "'.");
            this.listener = listener;
            playerListManager = this;
            room.start();
            room.touchAllElements();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> unsubscribe() {
            logger.debug("Unsubscribe request for '" + itemName + "'.");
            playerListManager = null;
            this.listener = null;
            room.stop();
            return CompletableFuture.completedFuture(null);
        }
    }

    private class BandwidthItemManager implements ItemManager {

        private final String itemName;
        private ItemListener listener;

        BandwidthItemManager(String itemName) {
            this.itemName = itemName;
        }

        @Override
        public Mode getSupportedMode() {
            return Mode.MERGE;
        }

        @Override
        public boolean isSnapshotAvailable() {
            return false;
        }

        @Override
        public CompletionStage<Void> subscribe(ItemListener listener) {
            this.listener = listener;
            bandManagers.put(itemName, this);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> unsubscribe() {
            bandManagers.remove(itemName);
            this.listener = null;
            room.killBandChecker(itemName);
            return CompletableFuture.completedFuture(null);
        }
    }

    // Public Methods Implementing Publisher Methods ---------------------------

    @Override
    public void publish(Event event) {

        try {

            PlayersListItemManager manager = playerListManager;
            if (manager == null) {
                return;
            }
            ItemListener listener = manager.listener;
            if (listener == null) {
                return;
            }

            logger.debug("Update list " + ITEM_NAME_PLAYERS_LIST + " " + event.getCommand() + " " + event.getKey());

            if (event.isSnapshot()) {
                listener.snapshot(event.getItemEvent(), null);
            } else {
                listener.update(event.getItemEvent());
            }

        } catch (Exception e) {
            logger.warn("Exception sending event.", e);
        }
    }

    @Override
    public void publishEOS() {

        PlayersListItemManager manager = playerListManager;
        if (manager == null) {
            return;
        }
        ItemListener listener = manager.listener;
        if (listener == null) {
            return;
        }

        // call the update on the listener;
        // in case the listener has just been detached,
        // the listener should detect the case
        listener.endOfSnapshot();
    }

    @Override
    public void postBandwith(String userName, Map<String, String> itemEvent) {
        BandwidthItemManager manager = bandManagers.get(ITEM_NAME_PREFIX_BAND + userName);
        if (manager == null) {
            return;
        }
        ItemListener listener = manager.listener;
        if (listener == null) {
            return;
        }

        listener.update(itemEvent);
    }

}