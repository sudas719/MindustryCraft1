package mindustry.type;

public enum UnitClass{
    biological("Biological Unit"),
    mechanical("Mechanical Unit"),
    heavy("Heavy Unit"),
    psionic("Psionic Unit"),
    hero("Hero Unit");

    public final String displayName;

    UnitClass(String displayName){
        this.displayName = displayName;
    }
}
