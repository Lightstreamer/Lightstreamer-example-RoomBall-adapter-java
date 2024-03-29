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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;

class WorldFactory {

    private static final int WORLD_SIZE_X = 1000;
    private static final int WORLD_SIZE_Y =  550;

    public static int  bRangeX = WORLD_SIZE_X;
    public static int  bRangeY = WORLD_SIZE_Y;
    private static final int  bThick = 50;
    private int  bSizeX = bRangeX + 2 * bThick;
    private int  bSizeY = bRangeY + 2 * bThick;

    private final Logger logger = LogManager.getLogger(RoomBallAdapter.ROOM_DEMO_LOGGER_NAME);

    public World createWorld(int world_size_x, int world_size_y) {
        if ( world_size_x != 0 ) {
            bRangeX = world_size_x;
            bSizeX = bRangeX + 2 * bThick;
        }
        
        if ( world_size_y != 0 ) {
            bRangeY = world_size_y;
            bSizeY = bRangeY + 2 * bThick;
        }
        
        Vec2  gravity = new Vec2(0, 0);
        World world = new World(gravity);
        addBoundariesTo(world);
        setBallContact(world);
        return world;
    }

    private void addBoundariesTo(World world) {
        createLeftBorder(world);
        createUpBorder(world);
        createRightBorder(world);
        createBottomBorder(world);
    }

    private void createLeftBorder(World world) {
        createBorderPolygon(world, bThick, bSizeY, - (bThick / 2), bRangeY / 2);
    }

    private void createUpBorder(World world) {
        createBorderPolygon(world, bSizeX, bThick, bRangeX / 2, - (bThick / 2));
    }

    private void createRightBorder(World world) {
        createBorderPolygon(world, bThick, bSizeY, bRangeX + (bThick / 2), bRangeY / 2);
    }

    private void createBottomBorder(World world) {
        createBorderPolygon(world, bSizeX, bThick, bRangeX / 2, bRangeY + (bThick / 2));
    }

    private void createBorderPolygon(World world, int dimx, int dimy, int posx, int posy) {
        PolygonShape polygonShape = new PolygonShape();
        polygonShape.setAsBox(dimx / 2, dimy / 2);

        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(posx, posy);
        bodyDef.allowSleep = false;
        Body body = world.createBody(bodyDef);

        FixtureDef fd = new FixtureDef();
        fd.shape = polygonShape;
        fd.density = 0.0f;
        fd.restitution = 0.1f;

        body.createFixture(fd);
    }

    private void setBallContact(World world) {
        world.setContactListener(new ContactListener() {

            @Override
            public void beginContact(Contact contact) {
                logger.debug("Begin contact");
                Object bodyInfoA = contact.getFixtureA().getBody().getUserData();
                Object bodyInfoB = contact.getFixtureB().getBody().getUserData();
                Body ball = null;
                if (bodyInfoA != null && bodyInfoA.equals(Ball.USERDATA_BALL)) {
                    ball = contact.getFixtureA().getBody();
                    if (bodyInfoB != null && bodyInfoB.equals(Player.USERDATA_PLAYER)) {
                        // scontro palla-player
                    } else {
                        return;
                    }
                } else if (bodyInfoB != null && bodyInfoB.equals(Ball.USERDATA_BALL)) {
                    ball = contact.getFixtureB().getBody();
                    if (bodyInfoA != null && bodyInfoA.equals(Player.USERDATA_PLAYER)) {
                        // scontro palla-player
                    } else {
                        return;
                    }
                }
                if (ball != null) {
                    ball.getFixtureList().setRestitution(2);
                }
            }

            @Override
            public void endContact(Contact contact) {
                logger.debug("End contact");
                Object bodyInfoA = contact.getFixtureA().getBody().getUserData();
                Object bodyInfoB = contact.getFixtureB().getBody().getUserData();
                Body ball = null;
                if (bodyInfoA != null && bodyInfoA.equals(Ball.USERDATA_BALL)) {
                    ball = contact.getFixtureA().getBody();
                } else if (bodyInfoB != null && bodyInfoB.equals(Ball.USERDATA_BALL)) {
                    ball = contact.getFixtureB().getBody();
                }
                if (ball != null) {
                    ball.getFixtureList().setRestitution(BallFactory.DEFAULT_BALL_RESTITUTION);
                    // NOTA: dovremmo anche assicurarci che non ci siano altri contatti contemporanei
                }
            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {
            }

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {
            }
        });
    }

}
