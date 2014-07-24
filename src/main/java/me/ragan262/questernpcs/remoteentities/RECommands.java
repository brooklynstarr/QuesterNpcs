package me.ragan262.questernpcs.remoteentities;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import me.ragan262.quester.commandmanager.annotations.Command;
import me.ragan262.quester.commandmanager.annotations.CommandLabels;
import me.ragan262.quester.commandmanager.context.CommandContext;
import de.kumpelblase2.remoteentities.EntityManager;
import de.kumpelblase2.remoteentities.api.RemoteEntity;
import de.kumpelblase2.remoteentities.api.RemoteEntityType;

public class RECommands {
	
	private final EntityManager npcManager;
	
	public RECommands(EntityManager npcManager) {
		this.npcManager = npcManager;
	}
	
	@CommandLabels({"list"})
	@Command(
			max = 0,
			desc = "displays the list of all entities")
	public void list(CommandContext context, CommandSender sender) {
		for(RemoteEntity e : npcManager.getAllEntities()) {
			sender.sendMessage("" + e.getID() + ". " + e.getName() + (e.isSpawned() ? "(spawned)" : "(despawned)"));
		}
	}
	
	@CommandLabels({"create"})
	@Command(
			min = 1,
			max = 1,
			usage = "<name> (-q)",
			desc = "creates new npc (quester)",
			player = true)
	public void onCreate(CommandContext context, CommandSender sender) {
		RemoteEntity created = npcManager.createNamedEntity(RemoteEntityType.Human, context.getPlayer().getLocation(), context.getString(0));
		created.setPushable(false);
		created.setStationary(true, false);
		if(context.hasFlag('q')) {
			created.getFeatures().addFeature(new QuesterFeature());
		}
		sender.sendMessage(ChatColor.GREEN + "Created entity id " + created.getID() + ".");
	}
	
	@CommandLabels({"remove"})
	@Command(
			min = 1,
			max = 1,
			usage = "<id>",
			desc = "removes an npc")
	public void remove(CommandContext context, CommandSender sender) {
		int id = context.getInt(0);
		if(npcManager.getRemoteEntityByID(id) == null) {
			sender.sendMessage(ChatColor.RED + "Specified entity not exist.");
			return;
		}
		npcManager.removeEntity(id);
		sender.sendMessage(ChatColor.GREEN + "Entity removed.");
	}
	
	@CommandLabels({"spawn"})
	@Command(
			min = 1,
			max = 1,
			usage = "<id>",
			desc = "spawns an npc to your location")
	public void spawn(CommandContext context, CommandSender sender) {
		RemoteEntity entity = npcManager.getRemoteEntityByID(context.getInt(0));
		if(entity == null) {
			sender.sendMessage(ChatColor.RED + "Specified entity not exist.");
			return;
		}
		if(entity.isSpawned()) {
			entity.teleport(context.getSenderLocation());
			sender.sendMessage(ChatColor.GREEN + "Entity moved.");
		}
		else {
			entity.spawn(context.getSenderLocation());
			sender.sendMessage(ChatColor.GREEN + "Entity spawned.");
		}
	}
	
	@CommandLabels({"XXX"})
	@Command(
			min = 1,
			max = 1,
			usage = "",
			desc = "",
			player = true)
	public void rename(CommandContext context, CommandSender sender) {
	}
}
