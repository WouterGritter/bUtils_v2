package nl.thewgbbroz.butils_v2.custominventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import nl.thewgbbroz.butils_v2.WGBPlugin;
import nl.thewgbbroz.butils_v2.services.WGBService;

public class SimpleGUIService extends WGBService implements Listener {
	private final WGBPlugin plugin;
	
	private Map<Player, SimpleGUIListener> inInv = new HashMap<>();
	
	public SimpleGUIService(WGBPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	@Override
	public void unload() {
		List<Player> players = new ArrayList<>(inInv.keySet());
		for(Player p : players) {
			p.closeInventory();
		}
		
		inInv.clear();
	}
	
	@EventHandler
	public void onPlayerQuitEvent(PlayerQuitEvent e) {
		if(inInv.containsKey(e.getPlayer())) {
			e.getPlayer().closeInventory();
			inInv.remove(e.getPlayer());
		}
	}
	
	@EventHandler
	public void onInventoryCloseEvent(InventoryCloseEvent e) {
		if(inInv.containsKey(e.getPlayer())) {
			try {
				inInv.get(e.getPlayer()).onClose(e.getInventory().getContents());
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			
			inInv.remove(e.getPlayer());
		}
		
		// Fix a (visual) bug when closing your inventory at the same tick as grabbing an item.
		// From the clients perspective the item is gone, while it is not.
		if(e.getPlayer() instanceof Player) {
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				Player p = (Player) e.getPlayer();
				p.updateInventory();
			}, 1);
		}
	}
	
	@EventHandler
	public void onInventoryClickEvent(InventoryClickEvent e) {
		// e.getInventory() is TOP inventory
		
		if(inInv.containsKey(e.getWhoClicked())) {
			SimpleGUIListener listener = inInv.get(e.getWhoClicked());
			
			boolean clickedInGUI = e.getRawSlot() >= 0 && e.getRawSlot() < e.getInventory().getSize();
			boolean shifting = e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY;
			if(listener.onClick(e.getCurrentItem(), e.getSlot(), clickedInGUI, shifting))
				e.setCancelled(true);
		}
	}
	
	/**
	 * Open an inventory.
	 */
	public Inventory openInventory(Player player, ItemStack[] contents, String title, SimpleGUIListener listener) {
		if(contents.length % 9 != 0)
			throw new IllegalArgumentException("contents.length must be divisible by 9!");
		
		Inventory inv = Bukkit.createInventory(null, contents.length, title);
		inv.setContents(contents);
		
		player.openInventory(inv);
		inInv.put(player, listener);
		
		return inv;
	}
	
	/**
	 * Open an inventory.
	 */
	public Inventory openInventory(Player player, Inventory inventory, SimpleGUIListener listener) {
		player.openInventory(inventory);
		inInv.put(player, listener);
		
		return inventory;
	}
	
	/**
	 * Closes all open custom inventories.
	 */
	public void closeInventories() {
		List<Player> players = new ArrayList<>(inInv.keySet());
		players.forEach(Player::closeInventory);
		
		// Make sure inInv map is empty
		inInv.clear();
	}
}
