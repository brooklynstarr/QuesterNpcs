package me.ragan262.questernpcs.remoteentities;

import java.util.List;

import me.ragan262.quester.ActionSource;
import me.ragan262.quester.QConfiguration;
import me.ragan262.quester.Quester;
import me.ragan262.quester.elements.Objective;
import me.ragan262.quester.elements.Trigger;
import me.ragan262.quester.elements.TriggerContext;
import me.ragan262.quester.holder.QuestHolder;
import me.ragan262.quester.holder.QuestHolderActionHandler;
import me.ragan262.quester.objectives.NpcKillObjective;
import me.ragan262.quester.objectives.NpcObjective;
import me.ragan262.quester.profiles.PlayerProfile;
import me.ragan262.quester.quests.Quest;
import me.ragan262.quester.utils.Util;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import de.kumpelblase2.remoteentities.EntityManager;
import de.kumpelblase2.remoteentities.api.DespawnReason;
import de.kumpelblase2.remoteentities.api.RemoteEntity;
import de.kumpelblase2.remoteentities.api.events.RemoteEntityDespawnEvent;
import de.kumpelblase2.remoteentities.api.events.RemoteEntityInteractEvent;

public class RemoteEntitiesListener extends QuestHolderActionHandler<RemoteEntity> implements Listener {
	
	private final EntityManager npcManager;
	
	public RemoteEntitiesListener(final Quester plugin, EntityManager npcManager) {
		super(plugin);
		this.npcManager = npcManager;
	}
	
	@Override
	public String getHeaderText(final Player player, final QuestHolder qh, final RemoteEntity data) {
		return data.getName() + "'s quests";
	}
	
	@Override
	public String getUsePermission() {
		return QConfiguration.PERM_USE_NPC;
	}
	
	@Override
	public void assignHolder(final QuestHolder qh, final RemoteEntity data) {
		data.getFeatures().getFeature(QuesterFeature.class).setHolderID(qh.getId());
	}
	
	@Override
	public void unassignHolder(final QuestHolder qh, final RemoteEntity data) {
		data.getFeatures().getFeature(QuesterFeature.class).setHolderID(-1);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBukkitEntityHit(final EntityDamageByEntityEvent event) {
		if(!(event.getEntity() instanceof LivingEntity) || !(event.getDamager() instanceof Player)) {
			return;
		}
		RemoteEntity entity = npcManager.getRemoteEntityFromEntity((LivingEntity) event.getEntity());
		if(entity == null) {
			return;
		}
		QuesterFeature feature = entity.getFeatures().getFeature(QuesterFeature.class);
		if(feature == null) {
			return;
		}
		Player player = (Player)event.getDamager();
		player.sendMessage("EntityDamageByEntityEvent - LEFT: " + true);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBukkitEntityClick(final PlayerInteractEntityEvent event) {
		if(!(event.getRightClicked() instanceof LivingEntity)) {
			return;
		}
		RemoteEntity entity = npcManager.getRemoteEntityFromEntity((LivingEntity) event.getRightClicked());
		if(entity == null) {
			return;
		}
		QuesterFeature feature = entity.getFeatures().getFeature(QuesterFeature.class);
		if(feature == null) {
			return;
		}
		event.getPlayer().sendMessage("PlayerInteractEntityEvent - LEFT: " + false);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityClick(final RemoteEntityInteractEvent event) {
		QuesterFeature feature = event.getRemoteEntity().getFeatures().getFeature(QuesterFeature.class);
		if(feature == null) {
			return;
		}
		event.getInteractor().sendMessage("RemoteEntityInteractEvent - LEFT: " + event.isLeftClick());
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onNpcInteract(final RemoteEntityInteractEvent event) {
		final Player player = event.getInteractor();
		final PlayerProfile prof = profMan.getProfile(player);
		final Quest quest = prof.getQuest();
		if(quest != null) {
			if(!quest.allowedWorld(player.getWorld().getName().toLowerCase())) {
				return;
			}
			final List<Objective> objs = quest.getObjectives();
			final TriggerContext context = new TriggerContext("NPC_CLICK");
			context.put("CLICKEDNPC", event.getRemoteEntity().getID());
			objectives:
			for(int i = 0; i < objs.size(); i++) {
				if(!profMan.isObjectiveActive(prof, i)) {
					continue;
				}
				// check triggers
				for(final int trigId : objs.get(i).getTriggers()) {
					final Trigger trig = quest.getTrigger(trigId);
					if(trig != null) {
						if(trig.evaluate(player, context) && objs.get(i).tryToComplete(player)) {
							profMan.incProgress(player, ActionSource.triggerSource(trig), i);
							break objectives;
						}
					}
				}
				// check objectives
				if(objs.get(i).getType().equalsIgnoreCase("NPC")) {
					final NpcObjective obj = (NpcObjective) objs.get(i);
					if(obj.checkNpc(event.getRemoteEntity().getID())) {
						profMan.incProgress(player, ActionSource.listenerSource(event), i);
						if(obj.getCancel()) {
							event.setCancelled(true);
						}
						return;
					}
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onNpcDeath(final RemoteEntityDespawnEvent event) {
		if(event.getReason() != DespawnReason.DEATH) {
			return;
		}
		final Player player = event.getRemoteEntity().getBukkitEntity().getKiller();
		if(player == null || !Util.isPlayer(player)) {
			return;
		}
		final PlayerProfile prof = profMan.getProfile(player);
		final Quest quest = prof.getQuest();
		if(quest != null) {
			if(!quest.allowedWorld(player.getWorld().getName().toLowerCase())) {
				return;
			}
			final List<Objective> objs = quest.getObjectives();
			for(int i = 0; i < objs.size(); i++) {
				if(objs.get(i).getType().equalsIgnoreCase("NPCKILL")) {
					if(!profMan.isObjectiveActive(prof, i)) {
						continue;
					}
					final NpcKillObjective obj = (NpcKillObjective) objs.get(i);
					if(obj.checkNpc(event.getRemoteEntity().getName())) {
						profMan.incProgress(player, ActionSource.listenerSource(event), i);
						return;
					}
				}
			}
		}
	}
}
