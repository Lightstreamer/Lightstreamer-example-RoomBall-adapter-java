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
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ua_parser.Client;
import ua_parser.Parser;

import com.lightstreamer.adapter.common.InitException;
import com.lightstreamer.adapter.metadata.CreditsException;
import com.lightstreamer.adapter.metadata.LoginInfo;
import com.lightstreamer.adapter.metadata.MetadataAdapter;
import com.lightstreamer.adapter.metadata.SessionListener;
import com.lightstreamer.adapter.metadata.SessionRequestHandler;
import com.lightstreamer.adapter.metadata.SubscriptionHandler;
import com.lightstreamer.adapter.metadata.SubscriptionInfo;

public class RoomBallMetaAdapter implements MetadataAdapter {

    public static final String ROOM_DEMO_LOGGER_NAME = "LS_demos_Logger.RoomBallDemo";
    
    public static final String TRACER_LOGGER = "LS_RoomBallDemo_Logger.tracer";

    private static final String FIELD_USER_AGENT = "USER_AGENT";
    
    private static final String FIELD_REMOTE_IP = "REMOTE_IP";

    private static final String CFG_PARAM_JMX_PORT = "jmxPort";

    private static final String ITEM_NAME_PREFIX_BAND = "My_Band_";

    private static final String ERROR = "Error";

    private static final String NAME_PREFIX_BALL = "Ball-";
    
    private static final String WORLD_SIZE_X = "world_size_x";
    
    private static final String WORLD_SIZE_Y = "world_size_y";
    
    private static final String MAX_PLAYERS = "max_players";

    private Logger logger;

    /**
     * Private logger; a specific "LS_RoomDemo_Logger" category
     * should be supplied by logback configuration.
     */
    private Logger tracer = null;

    /**
     * map of player's names indexed by session id.
     * There can be only one player per session.
     */
    private final ConcurrentHashMap<String, String> nicksns =
            new ConcurrentHashMap<String, String>();

    /**
     * The room where the players live
     */
    private Room room;

    /**
     * The name of the accepted Data Adapter, to ensure only one is supported.
     */
    private String dataAdapterName;

    /**
     * Max number of players in the room of Room-Ball demo
     */
    private int max_num_of_players = 200;
    
    /**
     * The Executor used to deliver the messages to the Room
     */
    private final ExecutorService messageDeliveryExecutor;

    private int jmxPort = 9999;

    // Inner Classes -----------------------------------------------------------

    class NotifyChatMessageTask implements Runnable {

        private final String sessionID;
        private final String message;
        private final String ip;

        public NotifyChatMessageTask(String sessionID, String message, String ip) {
            super();
            this.sessionID = sessionID;
            this.message = message;
            this.ip = ip;
        }

        @Override
        public void run() {
            synchronized(RoomBallMetaAdapter.this) {
                String playerName = nicksns.get(sessionID);
                if (playerName == null) {
                    // the message might have come too early; we cannot fulfill it
                    logger.warn("Received chat message from incomplete player (ip: " + ip + ").");
                    return;
                }
                tracer.info("RoomBall Chat Message from '" + playerName + "' (ip: " + ip + "), session '"+sessionID+"': " + message);

                room.updatePlayerMsg(playerName, message);
            }
        }
    }

    class NotifyCommandTask implements Runnable {

        private final String sessionID;
        private final String message;
        private final String ip;

        public NotifyCommandTask(String sessionID, String message, String ip) {
            super();
            this.sessionID = sessionID;
            this.message = message;
            this.ip = ip;
        }

        @Override
        public void run() {
            synchronized(RoomBallMetaAdapter.this) {
                String playerName = nicksns.get(sessionID);
                if (playerName == null) {
                    // the message might have come too early; we cannot fulfill it
                    logger.warn("Received command message from incomplete player (ip: " + ip + ").");
                    return;
                }
                tracer.debug("Received command message from player '" + playerName + "' (ip: " + ip + ").");

                room.dispatchCommand(playerName, message);
                logger.debug("Input command from user " + playerName + ": " + message);
            }
        }
    }

    // Constructor -------------------------------------------------------------

    public RoomBallMetaAdapter() {
        messageDeliveryExecutor = Executors.newSingleThreadExecutor();
    }

    // Public Methods ----------------------------------------------------------

    @Override
    public void init(Map<String, String> params, File configDir) throws InitException {

        int world_size_x = 0;
        int world_size_y = 0;
        
        logger = LogManager.getLogger(ROOM_DEMO_LOGGER_NAME);

        try{
            tracer = LogManager.getLogger(TRACER_LOGGER);
            tracer.info(TRACER_LOGGER + " start.");
        } catch (Exception e) {
            logger.warn("Error on tracer initialization.",  e);
        }
        
        if (params.containsKey(WORLD_SIZE_X)) {
            logger.info("World size (x) configured: " + params.get(WORLD_SIZE_X));
            world_size_x = Integer.parseInt(params.get(WORLD_SIZE_X));
        }
        
        if (params.containsKey(WORLD_SIZE_Y)) {
            logger.info("World size (y) configured: " + params.get(WORLD_SIZE_Y));
            world_size_y = Integer.parseInt(params.get(WORLD_SIZE_Y));
        }
        
        if (params.containsKey(MAX_PLAYERS)) {
            logger.info("World size (y) configured: " + params.get(MAX_PLAYERS));
            max_num_of_players = Integer.parseInt(params.get(MAX_PLAYERS));
        }
        
        room = Box2DRoom.getInstance(world_size_x, world_size_y);
        
        if (params.containsKey(CFG_PARAM_JMX_PORT)) {
            this.jmxPort = Integer.parseInt(params.get(CFG_PARAM_JMX_PORT));
        }
        logger.info("JMX Port:" + this.jmxPort);
    }

    @Override
    public CompletionStage<SessionRequestHandler> loginAsync(LoginInfo loginInfo) {
        //////////////////////////////////////////////////////////////////////////
        // WARNING: No authentication logic is implemented; any login is accepted.
        // This is suitable for demos only and MUST NOT be used in production.
        //////////////////////////////////////////////////////////////////////////
        String user = loginInfo.getUser();
        Map<String, String> clientContext = loginInfo.getClientContext();

        return CompletableFuture.completedFuture(new SessionRequestHandler() {

            private volatile String sessionID;
            private volatile boolean closed;
            private String ip;
            private String userAgent;

            @Override
            public Double getAllowedMaxBandwidth() {
                return null;
            }

            @Override
            public void onStart(SessionListener listener) {
                sessionID = listener.getSessionId();
                ip = clientContext.get(FIELD_REMOTE_IP);
                if (ip == null) {
                    logger.warn("Unable to retrieve IP for session '" + sessionID + "'.");
                    ip = "";
                }
                userAgent = parseUserAgent(sessionID, clientContext);
            }

            @Override
            public void onClose() {
                synchronized(RoomBallMetaAdapter.this) {
                    closed = true;

                    String deadmanwalking = nicksns.remove(sessionID);
                    if (deadmanwalking != null) {
                        try {
                            room.removePlayer(deadmanwalking);
                        } catch (Exception e) {
                            logger.warn(e);
                        }
                    } else {
                        logger.warn("Not exists " + sessionID + " in nicksns -> room.removePlayer not called!");
                    }
                }
            }

            @Override
            public CompletionStage<SubscriptionHandler> newSubscription(SubscriptionInfo info)
                    throws CreditsException {
                String adapterRef = info.getDataAdapterRef();
                if (!adapterRef.equals(dataAdapterName)) {
                    throw new CreditsException(-1, "Subscription to Data Adapter '" + adapterRef + "' not allowed.");
                }
                String itemName = info.getItemRefs().get(0);
                if ( itemName.startsWith(ITEM_NAME_PREFIX_BAND) ) {
                    String usr = itemName.substring(ITEM_NAME_PREFIX_BAND.length());
                    room.addPollsBandwidth(sessionID, itemName, usr, jmxPort);
                }
                return CompletableFuture.completedFuture(new SubscriptionHandler(info));
            }

            @Override
            public CompletionStage<String> userMessageAsync(String message)
                    throws CreditsException {

                // The message processing is done asynchronously in a fire-and-forget fashion.
                // As a consequence, we won't introduce blocking operations here,
                // hence we can proceed inline

                try {
                    if (message == null) {
                        return null;
                    }

                    if (closed) {
                        logger.warn("Message received from closed session '" + sessionID + "'.");
                        return null;
                    }

                    if (message.startsWith("n|") ) {

                        message = removeTypeFrom(message);

                        if (nicksns.size() >= max_num_of_players) {
                            logger.warn("Unable to add player: Room is overcrowded.");
                            throw new CreditsException(-2700, "Too many users. Please try again.");
                        }

                        final String proposedName = getNickNameFrom(message);
                        if (proposedName.equals(ERROR)) {
                            throw new CreditsException(-2710, "Error logging in.");
                        }

                        if (!isNaN(proposedName)) {
                            throw new CreditsException(-6, "I'm not a number! I'm a free man!");
                        }

                        if (proposedName.indexOf(NAME_PREFIX_BALL) == 0) {
                            throw new CreditsException(-1, "Srsly? A ball?");
                        }

                        if (nicksns.containsKey(sessionID)) {
                            // duplicated message; it should be avoided when possible
                            String actualNickName = nicksns.get(sessionID);

                            if (!actualNickName.equalsIgnoreCase(proposedName)) {
                                throw new CreditsException("Notifying nickname " + actualNickName + " to the client", -2720, actualNickName);
                                // brings back to the case where the name has been
                                // changed, causing the client to receive a non
                                // blocking error.
                            }
                        }

                        // synchronized login
                        String recommendedName;
                
                        synchronized(nicksns) {
                            recommendedName = getRecommendedName(proposedName, nicksns.values());
                	
                            try {
                                if (ip.isEmpty()) {
                                    logger.warn("New player '" + recommendedName + "' message received from non-existent session '" + sessionID + "'.");
                                } else {
                                    tracer.info("New player '" + recommendedName + "' from ip " + ip );
                                }
                	
                                room.addPlayer(recommendedName, userAgent);
                                nicksns.put(sessionID, recommendedName);
                            } catch (RoomException e) {
                                logger.warn("Unable to add player: " + e.getMessage());
                            } catch (Exception e) {
                                logger.warn("Message not well formatted, skipped.", e);
                            }
                        }
                
                        if (!recommendedName.equalsIgnoreCase(proposedName)) {
                            throw new CreditsException("Notifying nickname " + recommendedName + " to the client", -2720, recommendedName);
                            // brings back to the case where the name has been
                            // changed, causing the client to receive a non
                            // blocking error.
                        }
                    } else if ( message.startsWith("m|") ) {
                        message = removeTypeFrom(message);
                        messageDeliveryExecutor.execute(new NotifyChatMessageTask(sessionID, message, ip));
                    } else {
                        messageDeliveryExecutor.execute(new NotifyCommandTask(sessionID, message, ip));
                    }
                } catch (CreditsException e) {
                    throw e;
                } catch (Exception e) {
                    // Skip, message not well formatted
                    logger.warn("Unexpected error handling message from user '"+user+"', session '"+sessionID+"'.", e);
                }

                return CompletableFuture.completedFuture(null);
            }
        });
    }

    @Override
    public void onDataAdapterAvailable(String dataAdapterName, Object dataAdapter) {
        if (!(dataAdapter instanceof RoomBallAdapter)) {
            logger.warn("Data Adapter '" + dataAdapterName + "' has unexpected type: " + dataAdapter.getClass().getName());
            return;
        }
        if (this.dataAdapterName != null) {
            logger.warn("Data Adapter '" + dataAdapterName + "' ignored: already bound to '" + this.dataAdapterName + "'.");
            return;
        }
        this.dataAdapterName = dataAdapterName;
        ((RoomBallAdapter) dataAdapter).setRoom((Box2DRoom) room);
    }

    @Override
    public void onDataAdapterUnavailable(String dataAdapterName) {
        if (!dataAdapterName.equals(this.dataAdapterName)) {
            return;
        }
        logger.info("Data Adapter '" + dataAdapterName + "' is no longer available.");
        this.dataAdapterName = null;
        ((Box2DRoom) room).setPublisher(new NullPublisher());
    }

    @Override
    public void shutdown() {
        messageDeliveryExecutor.shutdown();
    }

    // Private Methods ---------------------------------------------------------

    private String parseUserAgent(String sessionID, Map<String, String> sessionInfo) {
        String ua = sessionInfo.get(FIELD_USER_AGENT);
        if (ua == null) {
            logger.info("User Agent not present for session " + sessionID);
            return "undetected";
        }

        logger.info("User Agent: " + ua);
        Parser uaParser = new Parser();
        Client c = uaParser.parse(ua);
        String userAgent = ( c.userAgent.family.equals("Android") ?
            c.userAgent.family + " Browser on " + c.os.family :
            c.userAgent.family + " on " + c.os.family);
        logger.info("Parsed: " + userAgent + ", for " + sessionID);
        return userAgent;
    }

    private String removeTypeFrom(String message) {
        String newMsg;
        try {
            newMsg = message.split("\\|")[1];
        } catch (ArrayIndexOutOfBoundsException aiobe) {
            // let's assume that the message ends with | to carry an empty value
            newMsg = "";
        }
        return newMsg;
    }

    private boolean isNaN(String str) {
        if (str.charAt(0) == '-') {
          str = str.substring(1);
        }

        int i=0;
        while (i<str.length() && Character.isDigit(str.charAt(i))) {
          i++;
        }
        return i != str.length();
    }

    private String getNickNameFrom(String message) {
        final String nickname = message;
        return nickname;
    }

    /**
     * Compute a name that is not already present in the name list.
     * The returned name is based on the proposed one.
     * @param proposedName The proposed name
     * @param namesNotAllowed The set of names form with the computed name must be
     * different.
     * @return a name not preset in namesNotAllowed
     */
    private String getRecommendedName(String proposedName, Collection<String> namesNotAllowed) {
        if (proposedName.startsWith(Ball.NAME_BALL)) {
            proposedName = "NotABall";
        }

        String recommendedName;
        if (!namesNotAllowed.contains(proposedName) ){
            recommendedName = proposedName;
        } else {
            int ik = 2;
            while ( namesNotAllowed.contains(proposedName+ik)  ){
                ik++;
            }

            recommendedName = proposedName+ik;
        }
        return recommendedName;
    }

}
