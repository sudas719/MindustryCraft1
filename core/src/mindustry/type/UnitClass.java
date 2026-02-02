package mindustry.type;

public enum UnitClass{
    biological("Biological Unit"),
    psionic("Psionic Unit"),
    mechanical("Mechanical Unit"),
    hero("Hero Unit");

    public final String displayName;

    UnitClass(String displayName){
        this.displayName = displayName;
    }
}
