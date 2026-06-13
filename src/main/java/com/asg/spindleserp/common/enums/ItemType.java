package com.asg.spindleserp.common.enums;

public enum ItemType {
    RAW_MATERIAL,    // Direct production input (flour, steel, fabric, chemicals)
    SEMI_FINISHED,   // Intermediate / WIP (dough, cut fabric, sub-assemblies)
    FINISHED_GOOD,   // Sellable output (biscuit, garment, furniture)
    SERVICE,         // Non-physical (consulting, freight, labour)
    SPARE_PART,      // Machine / equipment spare parts
    CONSUMABLE,      // Low-value non-inventory (gloves, tape, oil, packaging)
    MRO,             // Maintenance, Repair & Operations supplies
    GENERAL,         // Miscellaneous / uncategorised
    FIXED_ASSET      // Capitalised plant & equipment
}
