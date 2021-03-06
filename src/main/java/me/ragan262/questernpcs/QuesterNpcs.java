package me.ragan262.questernpcs;

import me.ragan262.quester.Quester;
import me.ragan262.quester.commandmanager.CommandManager;
import me.ragan262.questernpcs.citizens.Citizens2Listener;
import me.ragan262.questernpcs.citizens.QuesterTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitFactory;
import net.citizensnpcs.api.trait.TraitInfo;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class QuesterNpcs extends JavaPlugin {
	
	private CommandManager comManager = null;
	
	@Override
	public void onEnable() {
		if(!checkQuester()) {
			getPluginLoader().disablePlugin(this);
			return;
		}
		
		setupCitizens();
		
		// not ready yet
		//setupRemoteEntities();
		
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!sender.isOp()) {
			sender.sendMessage(ChatColor.RED + "You don't have permission for this.");
			return true;
		}
		if(comManager == null) {
			sender.sendMessage(ChatColor.RED + "RemoteEntities are not installed. No commands registered.");
			return true;
		}
		comManager.handleCommand(args, sender);
		
		return true;
	}
	
	// get proper quester versioning going and fix this one
	private boolean checkQuester() {
		try {
			Class.forName("me.ragan262.quester.Quester");
		}
		catch (final Exception e) {
			getLogger().severe("Quester not found. Disabling.");
			return false;
		}
		try {
			Class.forName("me.ragan262.quester.holder.QuesterTrait");
			getLogger().severe("Quester in use is too old, update to newer version. Disabling.");
		}
		catch (final Exception e) {
			return true;
		}
		return false;
	}
	
	private void setupCitizens() {
		try {
			Class.forName("net.citizensnpcs.api.CitizensAPI");
		}
		catch (final Exception e) {
			getLogger().info("Citizens 2 not found...");
			return;
		}
		
		final TraitFactory factory = CitizensAPI.getTraitFactory();
		final TraitInfo info = TraitInfo.create(QuesterTrait.class).withName("quester");
		factory.registerTrait(info);
		getServer().getPluginManager().registerEvents(new Citizens2Listener(Quester.getInstance()), this);
		getLogger().info("Citizens 2 found and hooked...");
	}
}
