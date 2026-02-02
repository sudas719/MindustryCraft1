package mindustry.type;

public enum ArmorType{
    none("No Armor"),
    light("Light Armor"),
    heavy("Heavy Armor");

    public final String displayName;

    ArmorType(String displayName){
        this.displayName = displayName;
    }
}
