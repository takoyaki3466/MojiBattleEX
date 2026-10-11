package jp.takoa.mojibattle.domain;

public enum PlayerId {
    ONE, TWO, THREE, FOUR;
    public PlayerId opponent() { return values()[(ordinal()+1)%values().length]; }
    public String label(){return (ordinal()+1)+"P";}
}
