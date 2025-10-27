public class Copiatoarele extends Electronice {
    private int p_ton;
    private FormatCopiere format_copiere;

    public Copiatoarele(String denumire, int nr_inv, double pret, String zona_magazin, Stare stare,
                        int p_ton, FormatCopiere format_copiere) {
        super("COPIATOR", denumire, nr_inv, (float) pret, zona_magazin, stare);
        this.p_ton = p_ton;
        this.format_copiere = format_copiere;
        this.setStare(stare);
        this.format_copiere = format_copiere;
    }

    public int getP_ton() {
        return p_ton;
    }

    public FormatCopiere getFormat_copiere() {
        return format_copiere;
    }

    @Override
    public void AfisareEchipamente() {
        super.AfisareEchipamente();
    }

}
