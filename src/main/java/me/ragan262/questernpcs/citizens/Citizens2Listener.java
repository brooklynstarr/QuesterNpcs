package me.ragan262.questernpcs.citizens;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import me.ragan262.questernpcs.citizens.QuesterTrait;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public class Citizens2Listener extends QuestHolderActionHandler<NPC> implements Listener {

	public Citizens2Listener(final Quester plugin) {
		super(plugin);
	}
	
	@Override
	public String getHeaderText(final Player player, final QuestHolder qh, final NPC data) {
		return data.getName() + "'s quests";
	}
	
	@Override
	public String getUsePermission() {
		return QConfiguration.PERM_USE_NPC;
	}
	
	@Override
	public void assignHolder(final QuestHolder qh, final NPC data) {
		data.getTrait(QuesterTrait.class).setHolderID(qh.getId());
	}
	
	@Override
	public void unassignHolder(final QuestHolder qh, final NPC data) {
		data.getTrait(QuesterTrait.class).setHolderID(-1);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onNpcLeftClick(final NPCLeftClickEvent event) {
		if(event.getNPC().hasTrait(QuesterTrait.class)) {
			final QuestHolder qh =
					holMan.getHolder(event.getNPC().getTrait(QuesterTrait.class).getHolderID());
			onLeftClick(event.getClicker(), qh, event.getNPC());
		}
	}

	private NPCRightClickEvent cancel = null;

	@EventHandler(priority = EventPriority.MONITOR)
	public void onNpcRightClick(final NPCRightClickEvent event) {
		if(cancel == event) {
			return;
		}
		if(event.getNPC().hasTrait(QuesterTrait.class)) {
			final QuestHolder qh =
					holMan.getHolder(event.getNPC().getTrait(QuesterTrait.class).getHolderID());
			onRightClick(event.getClicker(), qh, event.getNPC());
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onNpcInteract(final NPCRightClickEvent event) {
		final Player player = event.getClicker();
		final PlayerProfile prof = profMan.getProfile(player);
		final Quest quest = prof.getQuest();
		if(quest != null) {
			if(!quest.allowedWorld(player.getWorld().getName().toLowerCase())) {
				return;
			}
			final List<Objective> objs = quest.getObjectives();
			final TriggerContext context = new TriggerContext("NPC_CLICK");
			context.put("CLICKEDNPC", event.getNPC().getId());
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
					if(obj.checkNpc(event.getNPC().getId())) {
						profMan.incProgress(player, ActionSource.listenerSource(event), i);
						if(obj.getCancel()) {
							event.setCancelled(true);
							cancel = event;
						}
						return;
					}
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onNpcDeath(final NPCDeathEvent event) {
		Entity entity = event.getNPC().getEntity();
		if(!(entity instanceof LivingEntity)) {
			return;
		}
		Player player = ((LivingEntity)entity).getKiller();

		if(player == null && entity.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
			Entity damager = ((EntityDamageByEntityEvent)entity.getLastDamageCause()).getDamager();
			if(damager instanceof Tameable) {
				AnimalTamer owner = ((Tameable)damager).getOwner();
				if(owner instanceof Player) {
					player = (Player)owner;
				}
			}
			else if(damager instanceof Projectile) {
				ProjectileSource shooter = ((Projectile)damager).getShooter();
				if(shooter instanceof Player) {
					player = (Player)shooter;
				}
			}
		}

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
					if(obj.checkNpc(event.getNPC().getName())) {
						profMan.incProgress(player, ActionSource.listenerSource(event), i);
						return;
					}
				}
			}
		}
	}
}
