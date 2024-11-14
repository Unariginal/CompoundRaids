package me.unariginal.compoundraids.datatypes;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.unariginal.compoundraids.CompoundRaids;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.ArrayList;

public class Reward {
    private final String item;
    private final int count;
    private final String nbt;
    private final ArrayList<String> commands;
    private final double weight;

    public Reward(String item, int count, String nbt, ArrayList<String> commands, double weight) {
        this.item = item;
        this.count = count;
        this.nbt = nbt;
        this.commands = commands;
        this.weight = weight;
    }

    public ItemStack getItemStack() {
        ItemStack itemStack = getItem(item).getDefaultStack();
        itemStack.setCount(count);

        NbtCompound nbtComp;
        ComponentMap component;
        try {
            nbtComp = StringNbtReader.parse(nbt);
            component = ComponentMap.CODEC.decode(NbtOps.INSTANCE, nbtComp).result().get().getFirst();
            itemStack.applyComponentsFrom(component);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }

        return itemStack;
    }

    public ArrayList<String> getCommands() {
        return commands;
    }

    public double getWeight() {
        return weight;
    }

    private Item getItem(String namespace) {
        return CompoundRaids.getInstance().mcServer.getWorlds().iterator().next().getRegistryManager().get(RegistryKeys.ITEM).get(Identifier.of(namespace));
    }
}
