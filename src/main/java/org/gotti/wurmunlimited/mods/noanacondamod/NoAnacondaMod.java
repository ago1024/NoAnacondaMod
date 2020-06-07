package org.gotti.wurmunlimited.mods.noanacondamod;

import java.lang.reflect.Field;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import com.wurmonline.mesh.Tiles;
import com.wurmonline.mesh.Tiles.Tile;
import com.wurmonline.server.Items;
import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplateIds;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.ItemTypes;
import com.wurmonline.server.zones.Encounter;
import com.wurmonline.server.zones.EncounterType;
import com.wurmonline.server.zones.SpawnTable;

public class NoAnacondaMod implements WurmServerMod, ServerStartedListener, ItemTypes, MiscConstants {
	

	private static final Logger LOGGER = Logger.getLogger(NoAnacondaMod.class.getName());

	@Override
	public void onServerStarted() {
		LOGGER.info("onServerStarted");
		
		byte[] elevations = {
				EncounterType.ELEVATION_GROUND,
				EncounterType.ELEVATION_WATER,
				EncounterType.ELEVATION_DEEP_WATER,
				EncounterType.ELEVATION_FLYING,
				EncounterType.ELEVATION_FLYING_HIGH,
				EncounterType.ELEVATION_BEACH,
				EncounterType.ELEVATION_CAVES
		};
		
		Tile[] tiles = Tiles.Tile.values();
		
		try {

			Field fieldEncounters = ReflectionUtil.getField(EncounterType.class, "encounters");

			for (byte elevation : elevations) {
				for (Tile tile : tiles) {
					EncounterType encounterType = SpawnTable.getType(tile.id, elevation);
					if (encounterType != null) {
						List<Encounter> encounters = ReflectionUtil.getPrivateField(encounterType, fieldEncounters);
						ListIterator<Encounter> iterator = encounters.listIterator();
						while (iterator.hasNext()) {
							Encounter encounter = iterator.next();
							if (encounter.getTypes().containsKey(CreatureTemplateIds.ANACONDA_CID)) {
								Encounter newEncounter = new Encounter();
								final Map<Integer, Integer> types = newEncounter.getTypes();
								types.putAll(encounter.getTypes());
								types.remove(CreatureTemplateIds.ANACONDA_CID);
								if (types.isEmpty()) {
									types.put(CreatureTemplateIds.CAT_WILD_CID, 1);
								}
								iterator.set(newEncounter);
								LOGGER.info(String.format("Replacing anaconda encounter from tile %s, elevation %d", tile.getName(), elevation));
								LOGGER.info(newEncounter.toString());
							}
						}
					}
				}
			}
			
			int killed = 0;
			for (Creature creature : Creatures.getInstance().getCreatures()) {
				if (creature.getTemplate() != null && creature.getTemplate().getTemplateId() == CreatureTemplateIds.ANACONDA_CID) {
					creature.die(false, "NoAnacondaMod");
					killed++;
				}
			}
			LOGGER.info(String.format("Wiped %d anacondas from the server", killed));
			int destroyed = 0;
			for (Item item : Items.getAllItems()) {
				if (item.getTemplateId() == ItemList.corpse && item.getData1() == CreatureTemplateIds.ANACONDA_CID) {
					Items.destroyItem(item.getWurmId());
					destroyed++;
				}
			}
			LOGGER.info(String.format("Removed %d anaconda corpses from the server", destroyed));
		
		} catch (Exception e) {
			throw new HookException(e);
		}
	}
}
