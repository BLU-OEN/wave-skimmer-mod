package com.bluoen.waveskimmer;

import net.neoforged.neoforge.energy.EnergyStorage;

/** Energy buffer that can be filled internally but refuses energy from outside. */
public class GeneratorEnergyStorage extends EnergyStorage {
    public GeneratorEnergyStorage(int capacity, int maxExtract) {
        super(capacity, 0, maxExtract, 0);
    }

    public void generate(int amount) {
        energy = Math.min(capacity, energy + amount);
    }
}
