/*
 * CommandBook
 * Copyright (C) 2011 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.commandbook;

import com.sk89q.commandbook.components.AbstractComponent;
import com.sk89q.commandbook.components.ComponentInformation;
import com.sk89q.commandbook.components.InjectComponent;
import com.sk89q.commandbook.config.ConfigurationBase;
import com.sk89q.commandbook.config.Setting;
import com.sk89q.commandbook.events.core.BukkitEvent;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@ComponentInformation(friendlyName = "Thor", desc = "Thor's hammer and other lightning effects.")
public class ThorComponent extends AbstractComponent implements Listener {

    @InjectComponent private SessionComponent sessions;
    
    private LocalConfiguration config;

    private static Random random = new Random();
    
    @Override
    public void initialize() {
        this.config = configure(new LocalConfiguration());
        CommandBook.inst().getEventManager().registerEvents(this, this);
        registerCommands(Commands.class);
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("hammer-items") public Set<Integer> thorItems = new HashSet<Integer>(Arrays.asList(278, 285, 257, 270));
    }

    @BukkitEvent(type = Event.Type.PLAYER_INTERACT)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (sessions.getSession(player).hasThor()) {
            if (!config.thorItems.contains(player.getItemInHand().getTypeId())) {
                return;
            }

            if (event.getAction() == Action.LEFT_CLICK_AIR) {
                Block block = player.getTargetBlock(null, 300);
                if (block != null) {
                    player.getWorld().strikeLightning(block.getLocation());
                }
            } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                Block block = event.getClickedBlock();
                player.getWorld().strikeLightning(block.getLocation());
            }
        }
    }
    
    public class Commands {
        @Command(aliases = {"shock"}, usage = "[target]", desc = "Shock a player", flags = "ksa", min = 0, max = 1)
        @CommandPermissions({"commandbook.shock"})
        public void shock(CommandContext args, CommandSender sender) throws CommandException {
            Iterable<Player> targets = null;
            boolean included = false;
            int count = 0;

            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 0) {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));

                // Check permissions!
                CommandBook.inst().checkPermission(sender, "commandbook.shock");
            } else if (args.argsLength() == 1) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));

                // Check permissions!
                CommandBook.inst().checkPermission(sender, "commandbook.shock.other");
            }

            for (final Player player : targets) {
                count++;

                // Area effect
                if (args.hasFlag('a')) {
                    final Location origLoc = player.getLocation();

                    for (int i = 0; i < 10; i++) {
                        CommandBook.server().getScheduler().scheduleSyncDelayedTask(CommandBook.inst(), new Runnable() {
                            public void run() {
                                Location loc = origLoc.clone();
                                loc.setX(loc.getX() + random.nextDouble() * 20 - 10);
                                loc.setZ(loc.getZ() + random.nextDouble() * 20 - 10);
                                player.getWorld().strikeLightning(loc);
                            }
                        }, Math.max(0, i * 3 + random.nextInt(10) - 5));
                    }
                } else {
                    player.getWorld().strikeLightning(player.getLocation());
                }

                if (args.hasFlag('k')) {
                    player.setHealth(0);
                }

                if (args.hasFlag('s')) {
                    // Tell the user
                    if (player.equals(sender)) {
                        player.sendMessage(ChatColor.YELLOW + "Shocked!");

                        // Keep track of this
                        included = true;
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "You've been shocked by "
                                + PlayerUtil.toName(sender) + ".");

                    }
                } else {
                    if (count < 6) {
                        CommandBook.server().broadcastMessage(
                                ChatColor.YELLOW + PlayerUtil.toName(sender)
                                        + " shocked " + PlayerUtil.toName(player));
                    } else if (count == 6) {
                        CommandBook.server().broadcastMessage(
                                ChatColor.YELLOW + PlayerUtil.toName(sender)
                                        + " shocked more people...");
                    }
                }
            }

            // The player didn't get anything, then we need to send the
            // user a message so s/he know that something is indeed working
            if (!included && args.hasFlag('s')) {
                sender.sendMessage(ChatColor.YELLOW.toString() + "Players shocked.");
            }
        }

        @Command(aliases = {"thor"}, usage = "[target]", desc = "Give a player Thor power",
                flags = "", min = 0, max = 1)
        @CommandPermissions({"commandbook.thor"})
        public void thor(CommandContext args, CommandSender sender) throws CommandException {
            Iterable<Player> targets = null;
            boolean included = false;

            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 0) {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));

                // Check permissions!
                CommandBook.inst().checkPermission(sender, "commandbook.thor");
            } else if (args.argsLength() == 1) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));

                // Check permissions!
                CommandBook.inst().checkPermission(sender, "commandbook.thor.other");
            }

            for (final Player player : targets) {
                sessions.getSession(player).setHasThor(true);

                // Tell the user
                if (player.equals(sender)) {
                    player.sendMessage(ChatColor.YELLOW + "You have been granted the mighty power of Thor's hammer!");

                    // Keep track of this
                    included = true;
                } else {
                    player.sendMessage(ChatColor.YELLOW + "You have been granted the might power of Thor's hammer by "
                            + PlayerUtil.toName(sender) + ".");

                }
            }

            // The player didn't get anything, then we need to send the
            // user a message so s/he know that something is indeed working
            if (!included && args.hasFlag('s')) {
                sender.sendMessage(ChatColor.YELLOW.toString() + "Players given Thor's hammer.");
            }
        }

        @Command(aliases = {"unthor"}, usage = "[target]", desc = "Revoke a player's Thor power", flags = "", min = 0, max = 1)
        @CommandPermissions({"commandbook.thor"})
        public void unthor(CommandContext args, CommandSender sender) throws CommandException {
            Iterable<Player> targets = null;
            boolean included = false;

            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 0) {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));

                // Check permissions!
                CommandBook.inst().checkPermission(sender, "commandbook.thor");
            } else if (args.argsLength() == 1) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));

                // Check permissions!
                CommandBook.inst().checkPermission(sender, "commandbook.thor.other");
            }

            for (final Player player : targets) {
                sessions.getSession(player).setHasThor(false);

                // Tell the user
                if (player.equals(sender)) {
                    player.sendMessage(ChatColor.YELLOW + "You've lost Thor's hammer!");

                    // Keep track of this
                    included = true;
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Thor's hammer has been revoked from you by "
                            + PlayerUtil.toName(sender) + ".");

                }
            }

            // The player didn't get anything, then we need to send the
            // user a message so s/he know that something is indeed working
            if (!included && args.hasFlag('s')) {
                sender.sendMessage(ChatColor.YELLOW.toString() + "Thor's hammer revokved from players.");
            }
        }
    }
}
