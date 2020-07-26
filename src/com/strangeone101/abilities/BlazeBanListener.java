package com.strangeone101.abilities;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class BlazeBanListener implements Listener {

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking() && BendingPlayer.getBendingPlayer(event.getPlayer()).canBend(CoreAbility.getAbility(BanBlaze.class))) {
            new BanBlaze(event.getPlayer());
        }
    }
}
