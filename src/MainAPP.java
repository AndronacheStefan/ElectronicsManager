import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MainAPP
{
    static final List<Electronice> echipamente = new ArrayList<>();

    public static void main(String[] args) {
        TestDB.main(args); // verifică conexiunea
        citireDinBD();
        mainMenu();
    }

    public static void citireDinBD() {
        String sql = """
            SELECT e.id, e.tip, e.denumire, e.nr_inv, e.pret, e.stare,
                   z.nume AS zona_mag,
                   i.ppm, i.dpi, i.p_car, i.mod_tiparire,
                   c.p_ton, c.format_copiere,
                   s.tip_mon, s.vit_proc, s.c_hdd, s.sistem_operare
            FROM echipamente e
            JOIN zone z ON e.zona_id = z.id
            LEFT JOIN imprimante i ON e.id = i.id
            LEFT JOIN copiatoare c ON e.id = c.id
            LEFT JOIN sisteme s ON e.id = s.id
            ORDER BY e.id;
        """;

        echipamente.clear();

        try (Connection conn = DBConnection.connec()) {
            assert conn != null;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String tip = rs.getString("tip");
                    String denumire = rs.getString("denumire");
                    int nrInv = rs.getInt("nr_inv");
                    double pret = rs.getDouble("pret");
                    String zona = rs.getString("zona_mag");
                    Stare stare = Stare.valueOf(rs.getString("stare").toUpperCase());

                    switch (tip) {
                        case "IMPRIMANTA" -> echipamente.add(new Imprimante(
                                denumire, nrInv, pret, zona, stare,
                                rs.getInt("ppm"), rs.getInt("dpi"),
                                rs.getInt("p_car"),
                                ModTiparire.valueOf(rs.getString("mod_tiparire").toUpperCase())
                        ));
                        case "COPIATOR" -> echipamente.add(new Copiatoarele(
                                denumire, nrInv, pret, zona, stare,
                                rs.getInt("p_ton"),
                                FormatCopiere.valueOf(rs.getString("format_copiere").toUpperCase())
                        ));
                        case "SISTEM" -> echipamente.add(new SistemCalculatoare(
                                denumire, nrInv, pret, zona, stare,
                                rs.getString("tip_mon"),
                                rs.getDouble("vit_proc"),
                                rs.getInt("c_hdd"),
                                SistemOperare.valueOf(rs.getString("sistem_operare").toUpperCase())
                        ));
                    }
                }
                System.out.println("✅ Datele au fost încărcate din baza de date!");
            }
        } catch (SQLException e) {
            System.out.println("❌ Eroare la citirea din baza de date: " + e.getMessage());
        }
    }

    public static void mainMenu() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("1 - Afișează toate echipamentele");
            System.out.println("2 - Afișează imprimantele");
            System.out.println("3 - Afișează copiatoarele");
            System.out.println("4 - Afișează sistemele de calculatoare");
            System.out.println("5 - Adaugă echipament nou");
            System.out.println("6 - Modifică starea unui echipament");
            System.out.println("7 - Actualizează modul de tipărire pentru o imprimantă");
            System.out.println("8 - Actualizează formatul de copiere pentru un copiator");
            System.out.println("9 - Actualizează sistemul de operare pentru un sistem");
            System.out.println("10 - Afișează echipamentele vândute");
            System.out.println("0 - Ieșire");
            System.out.print("Alege: ");
            String optiune = scanner.nextLine().trim();

            switch (optiune) {

                case "1" -> {
                    if (echipamente.isEmpty())
                        System.out.println("⚠ Nicio înregistrare încărcată din baza de date!");
                    else
                        echipamente.forEach(Electronice::AfisareEchipamente);
                }
                case "2" -> {
                    for (Electronice e : echipamente) {
                        if(e instanceof Imprimante) {
                            ((Imprimante) e).AfisareEchipamente();
                    }
                    }
                }
                case "3" -> {
                    for (Electronice e : echipamente) {
                        if(e instanceof Copiatoarele) {
                            ((Copiatoarele) e).AfisareEchipamente();
                        }
                    }
                }
                case "4" -> {
                    for (Electronice e : echipamente) {
                        if(e instanceof SistemCalculatoare) {
                            ((SistemCalculatoare) e).AfisareEchipamente();
                        }
                    }
                }
                case "5" -> adaugaEchipamentNou(scanner);
                case "6" -> modificaStareEchipament(scanner);
                case "7" -> setareModDeScriere(scanner);
                case "8" -> setareModCopiere(scanner);
                case "9" -> instalareSistemCalculatoare(scanner);
                case "10" -> afisareEchipamenteVandute();
                case "0" -> {
                    System.out.println("👋 Ieșire din aplicație.");
                    return;
                }
                default -> System.out.println("⚠️ Opțiune invalidă!");
            }
        }
    }

    public static void adaugaEchipamentNou(Scanner scanner) {
        System.out.print("Tipul echipamentului (IMPRIMANTA / COPIATOR / SISTEM): ");
        String tip = scanner.nextLine().trim().toUpperCase();

        if (!tip.equals("IMPRIMANTA") && !tip.equals("COPIATOR") && !tip.equals("SISTEM")) {
            System.out.println("⚠️ Tip necunoscut. Operațiunea a fost anulată.");
            return;
        }

        System.out.print("Denumirea echipamentului: ");
        String denumire = scanner.nextLine().trim();
        if (denumire.isEmpty()) {
            System.out.println("⚠️ Denumirea nu poate fi goală.");
            return;
        }

        int nrInv = citesteInt(scanner, "Număr de inventar: ");
        double pret = citesteDouble(scanner, "Preț: ");

        System.out.print("Zona magazin: ");
        String zonaMagazin = scanner.nextLine().trim();
        if (zonaMagazin.isEmpty()) {
            System.out.println("⚠️ Zona magazin nu poate fi goală.");
            return;
        }

        Stare stare = citesteStare(scanner);

        String verificareNrInvSql = "SELECT id FROM echipamente WHERE nr_inv = ?";
        String insertEchipamentSql = "INSERT INTO echipamente (tip, denumire, nr_inv, pret, zona_mag, stare, zona_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.connec()) {
            if (conn == null) {
                System.out.println("❌ Conexiunea la baza de date nu a putut fi stabilită.");
                return;
            }

            boolean autoCommitInitial = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                try (PreparedStatement verificareStmt = conn.prepareStatement(verificareNrInvSql)) {
                    verificareStmt.setInt(1, nrInv);
                    try (ResultSet rs = verificareStmt.executeQuery()) {
                        if (rs.next()) {
                            System.out.println("⚠️ Există deja un echipament cu numărul de inventar introdus.");
                            conn.setAutoCommit(autoCommitInitial);
                            return;
                        }
                    }
                }

                int zonaId = gasesteSauCreeazaZona(conn, zonaMagazin);

                int echipamentId;
                try (PreparedStatement insertEchipamentStmt = conn.prepareStatement(insertEchipamentSql, Statement.RETURN_GENERATED_KEYS)) {
                    insertEchipamentStmt.setString(1, tip);
                    insertEchipamentStmt.setString(2, denumire);
                    insertEchipamentStmt.setInt(3, nrInv);
                    insertEchipamentStmt.setDouble(4, pret);
                    insertEchipamentStmt.setString(5, zonaMagazin);
                    insertEchipamentStmt.setString(6, stare.name());
                    insertEchipamentStmt.setInt(7, zonaId);
                    insertEchipamentStmt.executeUpdate();

                    try (ResultSet generatedKeys = insertEchipamentStmt.getGeneratedKeys()) {
                        if (!generatedKeys.next()) {
                            throw new SQLException("Nu s-a putut determina ID-ul noului echipament.");
                        }
                        echipamentId = generatedKeys.getInt(1);
                    }
                }

                Electronice echipamentCreat;

                switch (tip) {
                    case "IMPRIMANTA" -> {
                        int ppm = citesteInt(scanner, "Pagini pe minut: ");
                        int dpi = citesteInt(scanner, "DPI: ");
                        int pCar = citesteInt(scanner, "Capacitate cartuș: ");
                        ModTiparire modTiparire = citesteModTiparire(scanner);

                        String insertImprimantaSql = "INSERT INTO imprimante (id, ppm, dpi, p_car, mod_tiparire) VALUES (?, ?, ?, ?, ?)";
                        try (PreparedStatement stmt = conn.prepareStatement(insertImprimantaSql)) {
                            stmt.setInt(1, echipamentId);
                            stmt.setInt(2, ppm);
                            stmt.setInt(3, dpi);
                            stmt.setInt(4, pCar);
                            stmt.setString(5, modTiparire.name());
                            stmt.executeUpdate();
                        }

                        echipamentCreat = new Imprimante(denumire, nrInv, pret, zonaMagazin, stare, ppm, dpi, pCar, modTiparire);
                    }
                    case "COPIATOR" -> {
                        int pTon = citesteInt(scanner, "Pagini per toner: ");
                        FormatCopiere format = citesteFormatCopiere(scanner);

                        String insertCopiatorSql = "INSERT INTO copiatoare (id, p_ton, format_copiere) VALUES (?, ?, ?)";
                        try (PreparedStatement stmt = conn.prepareStatement(insertCopiatorSql)) {
                            stmt.setInt(1, echipamentId);
                            stmt.setInt(2, pTon);
                            stmt.setString(3, format.name());
                            stmt.executeUpdate();
                        }

                        echipamentCreat = new Copiatoarele(denumire, nrInv, pret, zonaMagazin, stare, pTon, format);
                    }
                    case "SISTEM" -> {
                        System.out.print("Tip monitor: ");
                        String tipMon = scanner.nextLine().trim();
                        double vitProc = citesteDouble(scanner, "Viteză procesor (GHz): ");
                        int capacitateHdd = citesteInt(scanner, "Capacitate HDD (GB): ");
                        SistemOperare sistemOperare = citesteSistemOperare(scanner);

                        String insertSistemSql = "INSERT INTO sisteme (id, tip_mon, vit_proc, c_hdd, sistem_operare) VALUES (?, ?, ?, ?, ?)";
                        try (PreparedStatement stmt = conn.prepareStatement(insertSistemSql)) {
                            stmt.setInt(1, echipamentId);
                            stmt.setString(2, tipMon);
                            stmt.setDouble(3, vitProc);
                            stmt.setInt(4, capacitateHdd);
                            stmt.setString(5, sistemOperare.name());
                            stmt.executeUpdate();
                        }

                        echipamentCreat = new SistemCalculatoare(denumire, nrInv, pret, zonaMagazin, stare, tipMon, vitProc, capacitateHdd, sistemOperare);
                    }
                    default -> throw new IllegalStateException("Tip de echipament necunoscut: " + tip);
                }

                conn.commit();
                echipamente.add(echipamentCreat);
                System.out.println("✅ Echipamentul a fost adăugat cu succes.");

            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.out.println("❌ Eroare la revenirea tranzacției: " + rollbackEx.getMessage());
                }
                throw e;
            } finally {
                conn.setAutoCommit(autoCommitInitial);
            }
        } catch (SQLException e) {
            System.out.println("❌ Eroare la adăugarea echipamentului: " + e.getMessage());
        }
    }

    public static void afisareEchipamenteVandute() {
        boolean gasit = false;
        for (Electronice e : echipamente) {
            if(e.getStare() == Stare.VANDUT)
            {
            e.AfisareEchipamente();
            gasit = true;
            }
        }
        if(!gasit)
        {
            System.out.println("Nu exista echipamente vandute.");
        }
    }

    public static void setareModCopiere(Scanner scanner) {
        System.out.print("Introdu numărul de inventar al copiatorului: ");
        int nrInv = scanner.nextInt();
        scanner.nextLine();

        String selectSql = "SELECT id, format_copiere FROM copiatoare WHERE id = ?";

        String updateSql = "UPDATE copiatoare SET format_copiere = ? WHERE id = ?";

        try (Connection conn = DBConnection.connec()) {

            int idCopiator = -1;
            String formatCurent = null;

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, nrInv);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        idCopiator = rs.getInt("id");
                        formatCurent = rs.getString("format_copiere");
                        System.out.println("Formatul curent de copiere: " + formatCurent);
                    } else {
                        System.out.println("⚠️ Copiatorul cu nr. inventar " + nrInv + " nu a fost găsit.");
                        return;
                    }
                }
            }

            System.out.print("Introdu noul format de copiere (A3 / A4): ");
            String noulFormat = scanner.nextLine().toUpperCase();

            if (!noulFormat.equals("A3") && !noulFormat.equals("A4")) {
                System.out.println("⚠️ Format invalid!");
                return;
            }

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, noulFormat);
                updateStmt.setInt(2, idCopiator);
                int rows = updateStmt.executeUpdate();

                if (rows > 0)
                    System.out.println("✅ Formatul de copiere a fost actualizat în baza de date!");
                else
                    System.out.println("⚠️ Actualizarea a eșuat — copiatorul nu a fost găsit.");
            }

        } catch (SQLException e) {
            System.out.println("❌ Eroare la actualizarea formatului de copiere: " + e.getMessage());
        }
    }

    public static void instalareSistemCalculatoare(Scanner scanner) {
        System.out.println("Introdu numărul de inventar al sistemului de calculatoare: ");
        int nrInv = scanner.nextInt();
        scanner.nextLine();

        String selectSql = "SELECT sistem_operare FROM sisteme WHERE id = ?";
        String updateSql = "UPDATE sisteme SET sistem_operare = ? WHERE id = ?";

        try (Connection conn = DBConnection.connec();) {
            PreparedStatement selectStmt = conn.prepareStatement(selectSql);
            {

                selectStmt.setInt(1, nrInv);
                ResultSet rs = selectStmt.executeQuery();

                if (rs.next()) {
                    String sistemCurent = rs.getString("sistem_operare");
                    System.out.println("Sistemul de operare curent: " + sistemCurent);
                    System.out.print("Introdu noul sistem de operare (WINDOWS / LINUX / MACOS): ");
                    String noulSistem = scanner.nextLine().toUpperCase();

                    if (!noulSistem.equals("WINDOWS") && !noulSistem.equals("LINUX") && !noulSistem.equals("MACOS")) {
                        System.out.println("⚠️ Sistem de operare invalid!");
                        return;
                    }

                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, noulSistem);
                        updateStmt.setInt(2, nrInv);
                        int rows = updateStmt.executeUpdate();

                        if (rows > 0) {
                            System.out.println("✅ Sistemul de operare a fost actualizat în baza de date!");
                        } else {
                            System.out.println("⚠️ Sistemul de calculatoare cu nr. inventar " + nrInv + " nu a fost găsit.");
                        }
                    }

                } else {
                    System.out.println("⚠️ Sistemul de calculatoare cu nr. inventar " + nrInv + " nu a fost găsit.");
                }
            }
        }catch (SQLException e){
            System.out.println("❌ Eroare la actualizarea sistemului de operare: " + e.getMessage());
        }
    }


    public static void setareModDeScriere(Scanner scanner)
    {
        System.out.println("Introdu numărul de inventar al imprimantei: ");
        int nrInv = scanner.nextInt();
        scanner.nextLine();

        String selectSql = "SELECT mod_tiparire FROM imprimante WHERE id = ?";
        String updateSql = "UPDATE imprimante SET mod_tiparire = ? WHERE id = ?";

        try (Connection conn = DBConnection.connec();)
        {
             PreparedStatement selectStmt = conn.prepareStatement(selectSql);{

            selectStmt.setInt(1, nrInv);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                String modCurent = rs.getString("mod_tiparire");
                System.out.println("Modul de tipărire curent: " + modCurent);
                System.out.print("Introdu noul mod de tipărire (COLOR / ALBNEGRU): ");
                String noulMod = scanner.nextLine().trim().toUpperCase();

                if (noulMod.equals("MONOCROM")) {
                    noulMod = "ALBNEGRU";
                }

                if (!noulMod.equals("COLOR") && !noulMod.equals("ALBNEGRU")) {
                    System.out.println("⚠️ Mod de tipărire invalid!");
                    return;
                }

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, noulMod);
                    updateStmt.setInt(2, nrInv);
                    int rows = updateStmt.executeUpdate();

                    if (rows > 0) {
                        System.out.println("✅ Modul de tipărire a fost actualizat în baza de date!");
                    } else {
                        System.out.println("⚠️ Imprimanta cu nr. inventar " + nrInv + " nu a fost găsită.");
                    }
                }

            } else {
                System.out.println("⚠️ Imprimanta cu nr. inventar " + nrInv + " nu a fost găsită.");
            }
        }
            } catch (SQLException e) {
            System.out.println("❌ Eroare la actualizarea modului de tipărire: " + e.getMessage());
        }
    }

    public static void modificaStareEchipament(Scanner scanner) {
        System.out.print("Introdu nr. inventar al echipamentului: ");
        int nrInv = scanner.nextInt();
        scanner.nextLine();

        String selectSql = "SELECT stare FROM echipamente WHERE id = ?";
        String updateSql = "UPDATE echipamente SET stare = ? WHERE id = ?";

        try (Connection conn = DBConnection.connec();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {

            selectStmt.setInt(1, nrInv);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                String stareCurenta = rs.getString("stare");
                System.out.println("Starea curentă: " + stareCurenta);
                System.out.print("Introdu noua stare (ACHIZITIONAT / EXPUS / VANDUT): ");
                String nouaStare = scanner.nextLine().toUpperCase();

                // verifică dacă e o stare validă
                if (!nouaStare.equals("ACHIZITIONAT") &&
                        !nouaStare.equals("EXPUS") &&
                        !nouaStare.equals("VANDUT")) {
                    System.out.println("⚠️ Stare invalidă!");
                    return;
                }

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, nouaStare);
                    updateStmt.setInt(2, nrInv);
                    int rows = updateStmt.executeUpdate();

                    if (rows > 0) {
                        System.out.println("✅ Starea echipamentului a fost actualizată în baza de date!");
                    } else {
                        System.out.println("⚠️ Echipament cu nr. inventar " + nrInv + " nu a fost găsit.");
                    }
                }

            } else {
                System.out.println("⚠️ Echipament cu nr. inventar " + nrInv + " nu a fost găsit.");
            }

        } catch (SQLException e) {
            System.out.println("❌ Eroare la actualizarea stării: " + e.getMessage());
        }
    }

    private static int gasesteSauCreeazaZona(Connection conn, String numeZona) throws SQLException {
        String selectSql = "SELECT id FROM zone WHERE UPPER(nume) = UPPER(?)";
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, numeZona);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        String insertSql = "INSERT INTO zone (nume) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, numeZona);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        throw new SQLException("Nu s-a putut determina ID-ul pentru zona " + numeZona);
    }

    private static int citesteInt(Scanner scanner, String mesaj) {
        while (true) {
            System.out.print(mesaj);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Introdu un număr întreg valid.");
            }
        }
    }

    private static double citesteDouble(Scanner scanner, String mesaj) {
        while (true) {
            System.out.print(mesaj);
            String input = scanner.nextLine().trim().replace(',', '.');
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Introdu un număr zecimal valid.");
            }
        }
    }

    private static Stare citesteStare(Scanner scanner) {
        while (true) {
            System.out.print("Stare (ACHIZITIONAT / EXPUS / VANDUT): ");
            String input = scanner.nextLine().trim().toUpperCase();
            try {
                return Stare.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("⚠️ Stare invalidă. Încearcă din nou.");
            }
        }
    }

    private static ModTiparire citesteModTiparire(Scanner scanner) {
        while (true) {
            System.out.print("Mod tipărire (COLOR / ALBNEGRU): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("MONOCROM")) {
                input = "ALBNEGRU";
            }
            try {
                return ModTiparire.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("⚠️ Mod de tipărire invalid. Încearcă din nou.");
            }
        }
    }

    private static FormatCopiere citesteFormatCopiere(Scanner scanner) {
        while (true) {
            System.out.print("Format copiere (A3 / A4): ");
            String input = scanner.nextLine().trim().toUpperCase();
            try {
                return FormatCopiere.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("⚠️ Format invalid. Încearcă din nou.");
            }
        }
    }

    private static SistemOperare citesteSistemOperare(Scanner scanner) {
        while (true) {
            System.out.print("Sistem de operare (WINDOWS / LINUX / MACOS): ");
            String input = scanner.nextLine().trim().toUpperCase();
            try {
                return SistemOperare.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("⚠️ Sistem de operare invalid. Încearcă din nou.");
            }
        }
    }
}
