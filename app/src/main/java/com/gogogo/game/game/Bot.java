package com.gogogo.game.game;

import com.gogogo.game.engine.Rng;

/** Simple, slightly goofy opponent brain. */
public final class Bot {
    public final float reaction;   // seconds before reacting to a new color
    public final float mistake;    // chance to go for a wrong color
    public final float aggression; // likes ramming people off islands
    public final float smarts;     // avoids crowds, plans boost
    public final float wits;       // how well it uses power-ups
    public boolean dumb;           // dev option

    public Arena.Tile goal;
    public float thinkT;
    private int seenRound = -1;
    public boolean confused;
    private float wobble;
    private float rethinkT;
    private int victim = -1;

    public Bot(Rng rng, int tier) {
        switch (tier) {
            case 0: // noob
                reaction = rng.range(0.55f, 1.05f);
                mistake = rng.range(0.07f, 0.14f);
                aggression = rng.range(0f, 0.3f);
                smarts = rng.range(0f, 0.3f);
                wits = rng.range(0.15f, 0.45f);
                jumpLead = rng.range(0.15f, 0.8f);
                break;
            case 2: // pro
                reaction = rng.range(0.16f, 0.32f);
                mistake = rng.range(0.0f, 0.02f);
                aggression = rng.range(0.4f, 1f);
                smarts = rng.range(0.7f, 1f);
                wits = rng.range(0.8f, 1f);
                jumpLead = rng.range(0.1f, 0.3f);
                break;
            default:
                reaction = rng.range(0.3f, 0.6f);
                mistake = rng.range(0.02f, 0.06f);
                aggression = rng.range(0.1f, 0.7f);
                smarts = rng.range(0.3f, 0.7f);
                wits = rng.range(0.45f, 0.8f);
                jumpLead = rng.range(0.1f, 0.45f);
                break;
        }
        wobble = rng.range(0f, 100f);
    }

    public void think(Match m, Car c, float dt) {
        if (c.falling) {
            // only while a super jump can still get it out of the fall
            c.inX = c.inZ = 0;
            rescue(m, c);
            return;
        }
        rescueAt = -1f;
        drive(m, c, dt);
        if (c.airborne) airSteer(m, c);
        else if (c.power >= 0 && !dumb) usePower(m, c, dt);
        else powerT = -1f;
    }

    // ------------------------------------------------------------------ power-ups

    private final float jumpLead;   // seconds before the drop it jumps when stranded on a wrong color
    private float powerT = -1f;     // countdown to using the held power-up (-1 = nothing in sight)
    private float scanT;            // seconds until it looks around again
    private boolean seen;           // something worth using the power-up on was in sight at the last look
    private float rescueAt = -1f;   // seconds into a fall it jumps out (-1 = not decided yet)

    private void usePower(Match m, Car c, float dt) {
        if (m.phase != Match.SHOW && m.phase != Match.DROP || c.frozenT > 0) return;
        scanT -= dt;
        int type = c.power;
        if (type == PowerUps.JUMP) {
            // the escape hatch: stranded on a wrong color with the drop about to happen
            if (m.phase != Match.SHOW || m.timer > jumpLead) return;
            Arena.Tile here = m.arena.cellAt(c.x, c.z);
            if (here != null && here.state == Arena.PRESENT && here.color == m.target) return;
            float left = goal == null || goal.color != m.target ? 99f : dist(c, goal) - Arena.PITCH * 0.45f;
            if (left > Math.max(2f, c.speed()) * m.timer) c.wantPower = true;
            return;
        }
        if (type == PowerUps.STICKY) {
            if (c.speed() > c.maxSpeed * 0.75f && m.rng.chance(dt * (0.3f + wits))) c.wantPower = true;
            return;
        }
        if (scanT <= 0f) {
            scanT = m.rng.range(0.1f, 0.2f);
            seen = worthIt(m, c, type);
            if (!seen) powerT = -1f;
            else if (powerT < 0f) powerT = reaction * m.rng.range(0.7f, 1.5f) + (1f - wits) * m.rng.range(0.2f, 1.2f);
        }
        if (powerT >= 0f && seen) {
            powerT -= dt;
            if (powerT <= 0f) {
                c.wantPower = true;
                powerT = -1f;
            }
        }
    }

    /** Is there something to throw at / shove right now? */
    private boolean worthIt(Match m, Car c, int type) {
        if (type == PowerUps.SNOWBALL) return m.power.snowTarget(c) >= 0;
        if (type == PowerUps.ICE) return m.power.iceTarget(c) >= 0;
        // super bump: a crowd, or anyone close while the floor is dropping and this car stands near an edge
        Arena.Tile here = m.arena.cellAt(c.x, c.z);
        boolean edgy = m.phase == Match.DROP && here != null && here.state == Arena.PRESENT;
        float r = PowerUps.SHOCK_RADIUS * 0.8f;
        int near = 0;
        for (Car o : m.cars) {
            if (o == c || !o.alive || o.falling || o.airborne) continue;
            float dx = o.x - c.x, dz = o.z - c.z;
            if (dx * dx + dz * dz < r * r) near++;
        }
        return near >= 3 || (near >= 2 && m.rng.chance(0.4f + wits * 0.4f)) || (edgy && near >= 1 && m.rng.chance(0.3f * wits));
    }

    /** Just started falling while holding a super jump: jump out, if it reacts in time. */
    private void rescue(Match m, Car c) {
        if (c.power != PowerUps.JUMP || dumb) return;
        if (rescueAt < 0f) {
            // the sharper the bot, the more likely it remembers the jump at all, and the quicker it reacts
            rescueAt = m.rng.chance(0.2f + 0.75f * wits) ? m.rng.range(0.04f, 0.16f) + reaction * (0.45f - wits * 0.25f) : 9f;
        }
        if (c.fallT >= rescueAt) c.wantPower = true;
    }

    /** In the air: drift towards floor (over a hole or off the map: towards the nearest tile of the map). */
    private void airSteer(Match m, Car c) {
        Arena a = m.arena;
        // where it comes down if it just lets go
        float lx = c.x + c.vx * 0.6f, lz = c.z + c.vz * 0.6f;
        if (a.hasAt(lx, lz) && a.hasAt(c.x, c.z)) {
            if (m.phase == Match.DROP) c.inX = c.inZ = 0;
            return;
        }
        Arena.Tile t = a.nearest(c.x, c.z);
        if (t == null) return;
        float dx = t.x - c.x, dz = t.z - c.z;
        float d = (float) Math.sqrt(dx * dx + dz * dz) + 0.001f;
        c.inX = dx / d;
        c.inZ = dz / d;
    }

    // ------------------------------------------------------------------ driving

    private void drive(Match m, Car c, float dt) {
        Arena a = m.arena;
        wobble += dt;
        if (m.phase == Match.INTRO || m.phase == Match.OVER) {
            c.inX = c.inZ = 0;
            return;
        }
        if (m.phase == Match.SHOW) {
            if (seenRound != m.round) {
                seenRound = m.round;
                thinkT = reaction * (dumb ? 2.5f : 1f) * m.rng.range(0.85f, 1.2f);
                goal = null;
                confused = m.rng.chance(dumb ? 0.6f : mistake + Math.min(0.04f, m.round * 0.002f));
                victim = -1;
            }
            if (thinkT > 0) {
                thinkT -= dt;
                // drift a little while "thinking" (but not off the floor)
                float wx = (float) Math.sin(wobble * 1.3f) * 0.15f, wz = (float) Math.cos(wobble * 1.1f) * 0.15f;
                if (!a.hasAt(c.x + wx * 20f, c.z + wz * 20f)) wx = wz = 0f;
                c.inX = wx;
                c.inZ = wz;
                return;
            }
            if (goal == null || goal.state != Arena.PRESENT) pickGoal(m, c);
            // smart bots realize their mistake
            if (confused && goal != null && goal.color != m.target) {
                rethinkT += dt;
                if (rethinkT > 0.6f && m.rng.chance(smarts * 0.04f)) {
                    confused = false;
                    rethinkT = 0;
                    pickGoal(m, c);
                }
            }
            Arena.Tile here = a.cellAt(c.x, c.z);
            boolean safeHere = here != null && here.color == m.target && here.state == Arena.PRESENT;
            if (safeHere && !confused && goal != here && dist(c, goal) > Arena.PITCH * 0.9f) {
                // already safe: only move if the goal is close, otherwise settle
                goal = here;
            }
            if (goal != null && aggression > 0.55f && m.round >= 4 && safeHere && m.timer < 1.4f && !dumb) {
                seekVictim(m, c, here);
                if (victim >= 0) return;
            }
            steerTo(m, c, goal, true, dt);
        } else {
            // islands only: stay central, maybe shove someone
            Arena.Tile here = a.cellAt(c.x, c.z);
            if (here != null && here.state == Arena.PRESENT) {
                if (aggression > 0.6f && !dumb && m.round >= 4 && m.phase == Match.DROP && m.phaseT > 0.4f) {
                    seekVictim(m, c, here);
                    if (victim >= 0) return;
                }
                steerTo(m, c, here, false, dt);
            } else {
                c.inX = c.inZ = 0;
            }
        }
    }

    private void seekVictim(Match m, Car c, Arena.Tile here) {
        if (victim >= 0) {
            Car v = m.cars[victim];
            if (!v.alive || m.arena.cellAt(v.x, v.z) != here) victim = -1;
        }
        float ramp = Math.min(1f, (m.round - 3) / 6f);
        if (victim < 0 && m.rng.chance(0.02f * ramp)) {
            float best = 9f;
            for (Car o : m.cars) {
                if (o == c || !o.alive) continue;
                float dx = o.x - c.x, dz = o.z - c.z;
                float d = dx * dx + dz * dz;
                if (d < best && m.arena.cellAt(o.x, o.z) == here) {
                    best = d;
                    victim = o.index;
                }
            }
        }
        if (victim >= 0) {
            Car v = m.cars[victim];
            // push the victim away from the tile center
            float px = v.x - here.x, pz = v.z - here.z;
            float pl = (float) Math.sqrt(px * px + pz * pz) + 0.001f;
            float tx = v.x - px / pl * 1.2f, tz = v.z - pz / pl * 1.2f;
            float dx = tx - c.x, dz = tz - c.z;
            float d = (float) Math.sqrt(dx * dx + dz * dz) + 0.001f;
            boolean close = d < 1.4f;
            if (close) {
                dx = v.x - c.x;
                dz = v.z - c.z;
                d = (float) Math.sqrt(dx * dx + dz * dz) + 0.001f;
            }
            c.inX = dx / d;
            c.inZ = dz / d;
            // don't ram yourself off the edge, and only boost into someone with floor behind them
            float ex = c.x + c.inX * 2.5f, ez = c.z + c.inZ * 2.5f;
            if (m.arena.cellAt(ex, ez) != here) {
                victim = -1;
            } else if (close && c.boostCd <= 0 && m.rng.chance(0.08f)
                    && m.arena.clearLine(c.x, c.z, c.x + c.inX * 7f, c.z + c.inZ * 7f, 0.5f)) {
                c.wantBoost = true;
            }
        }
    }

    private static float dist(Car c, Arena.Tile t) {
        if (t == null) return 999f;
        float dx = t.x - c.x, dz = t.z - c.z;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    private void pickGoal(Match m, Car c) {
        Arena a = m.arena;
        Arena.Tile best = null;
        float bestScore = Float.MAX_VALUE;
        int want = m.target;
        if (confused) {
            // goofy: chase some other color nearby
            want = (m.target + 1 + m.rng.i(Math.max(1, m.numColors - 1))) % m.numColors;
        }
        // on maps with holes, tiles across a gap are farther than they look
        Arena.Tile here = a.full ? null : a.cellAt(c.x, c.z);
        if (here != null && !here.exists) here = null;
        for (Arena.Tile t : a.tiles) {
            if (t.state != Arena.PRESENT || t.color != want) continue;
            float dx = t.x - c.x, dz = t.z - c.z;
            float d = (float) Math.sqrt(dx * dx + dz * dz);
            if (here != null) {
                int s = a.steps(t.index, here.index); // one search from here serves every tile
                if (s < 0) continue;
                d += (s - Math.abs(t.gx - here.gx) - Math.abs(t.gz - here.gz)) * Arena.PITCH;
            }
            float score = d + t.crowd * (1.5f + 3f * smarts);
            // a power-up lying there is a nice bonus for empty hands
            if (c.power < 0 && m.power.tilePick[t.index] >= 0) score -= 2f + 2.5f * smarts;
            // avoid tiles at the map's outline or next to a hole a bit
            if (t.edge) score += 2f * smarts;
            if (score < bestScore) {
                bestScore = score;
                best = t;
            }
        }
        if (goal != null) goal.crowd = Math.max(0, goal.crowd - 1);
        goal = best;
        if (goal != null) goal.crowd++;
        aim = m.rng.range(-0.25f, 0.25f) * Arena.PITCH;
        aim2 = m.rng.range(-0.25f, 0.25f) * Arena.PITCH;
    }

    private float aim, aim2;

    // path following around the empty cells of a map
    private static final float MARGIN = 1.1f; // keep the car's middle this far from empty cells
    private int wayCell = -1;                 // tile to drive at first, -1 = straight at the goal
    private int wayFrom = -1, wayGoal = -1;   // car cell and goal the waypoint was found for
    private float wayT;                       // seconds until the waypoint is looked at again

    /**
     * Where to drive on the way to tile t: -1 = straight at (gx, gz), else the farthest tile along
     * the walk around the holes that the car can reach in a straight line.
     */
    private int route(Arena a, Car c, Arena.Tile t, float gx, float gz, float dt) {
        if (a.full) return -1;
        Arena.Tile here = a.cellAt(c.x, c.z);
        if (here == null || !here.exists || here == t) return -1;
        wayT -= dt;
        if (here.index == wayFrom && t.index == wayGoal && wayT > 0) return wayCell;
        wayFrom = here.index;
        wayGoal = t.index;
        wayT = 0.25f;
        wayCell = -1;
        if (a.clearLine(c.x, c.z, gx, gz, MARGIN)) return -1;
        int best = a.nextStep(here.index, t.index);
        if (best < 0 || best == t.index) return -1;
        // pull the string: skip ahead while the next tile is still in clear sight
        int cur = best;
        while (true) {
            int next = a.nextStep(cur, t.index);
            if (next < 0) break;
            if (next == t.index) {
                if (a.clearLine(c.x, c.z, gx, gz, MARGIN)) best = -1;
                break;
            }
            Arena.Tile nt = a.tiles[next];
            if (!a.clearLine(c.x, c.z, nt.x, nt.z, MARGIN)) break;
            best = cur = next;
        }
        wayCell = best;
        return best;
    }

    private void steerTo(Match m, Car c, Arena.Tile t, boolean rush, float dt) {
        if (t == null) {
            c.inX = c.inZ = 0;
            return;
        }
        float ax = aim, az = aim2;
        if (t.edge) {
            // park on the side away from the drop
            Arena a = m.arena;
            if (!a.has(t.gx - 1, t.gz)) ax = Math.abs(ax);
            else if (!a.has(t.gx + 1, t.gz)) ax = -Math.abs(ax);
            if (!a.has(t.gx, t.gz - 1)) az = Math.abs(az);
            else if (!a.has(t.gx, t.gz + 1)) az = -Math.abs(az);
        }
        float gx = t.x + ax, gz = t.z + az;
        int pk = c.power < 0 && t.state == Arena.PRESENT ? m.power.tilePick[t.index] : -1;
        if (pk >= 0) {
            // grab the power-up lying on the goal tile on the way
            gx = m.power.pickX[pk];
            gz = m.power.pickZ[pk];
        }
        int way = route(m.arena, c, t, gx, gz, dt);
        if (way >= 0) {
            // on the way around a hole: full speed at the waypoint, easing off only when the goal is right behind it
            Arena.Tile w = m.arena.tiles[way];
            float dx = w.x - c.x, dz = w.z - c.z;
            float d = (float) Math.sqrt(dx * dx + dz * dz) + 0.001f;
            float nx = dx / d, nz = dz / d;
            float left = d + m.arena.steps(way, t.index) * Arena.PITCH * 0.85f;
            float facing = (float) (Math.sin(c.yaw) * nx + Math.cos(c.yaw) * nz);
            // a light touch still steers but hardly pushes, so the car rolls to a stop around the corner
            float mag = left < c.speed() / 2.6f && c.vx * nx + c.vz * nz > 2f ? 0.1f : 1f;
            c.inX = nx * mag;
            c.inZ = nz * mag;
            if (rush && !dumb && c.boostCd <= 0 && m.timer > 0 && d > 12f) {
                float need = left / Math.max(4f, c.maxSpeed);
                if (facing > 0.95f && need > m.timer * 0.75f) c.wantBoost = true;
            }
            return;
        }
        float dx = gx - c.x, dz = gz - c.z;
        float d = (float) Math.sqrt(dx * dx + dz * dz);
        if (d < 0.6f) {
            c.inX = c.inZ = 0;
            return;
        }
        float sp = c.speed();
        // coasting stops in about speed / drag units: let go of the stick in time
        float stopDist = sp / 2.6f;
        float nx = dx / d, nz = dz / d;
        float closing = (c.vx * nx + c.vz * nz);
        float mag;
        if (d < stopDist * 0.9f && closing > 2f) mag = 0f;
        else mag = Math.min(1f, 0.35f + d / 4f);
        c.inX = nx * mag;
        c.inZ = nz * mag;
        if (rush && !dumb && c.boostCd <= 0 && m.timer > 0) {
            float need = d / Math.max(4f, c.maxSpeed);
            float facing = (float) (Math.sin(c.yaw) * nx + Math.cos(c.yaw) * nz);
            if (facing > 0.9f && d > 9f && (need > m.timer * 0.75f || m.rng.chance(0.01f * smarts))) c.wantBoost = true;
        }
    }

    public void forget() {
        goal = null;
        seenRound = -1;
    }
}
