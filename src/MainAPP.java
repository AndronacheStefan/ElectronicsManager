import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MainAPP
{
    static List<Electronice> echipamente = new ArrayList<>();

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

        try (Connection conn = DBConnection.connec()) {
            assert conn != null;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {\
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
            System.out.println("0 - Ieșire");
            System.out.print("Alege: ");
            switch (scanner.next()) {

                case "1" -> {
                    if (echipamente.isEmpty())
                        System.out.println("⚠️ Nicio înregistrare încărcată din baza de date!");
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
                case "5" -> modificaStareEchipament(scanner);
                case "6" -> setareModDeScriere(scanner);
                case "7" -> setareModCopiere(scanner);
                case "8" -> instalareSistemCalculatoare(scanner);
                case "9" -> afisareEchipamenteVandute();
                case "0" -> {
                    System.out.println("👋 Ieșire din aplicație.");
                    return;
                }
                default -> System.out.println("⚠️ Opțiune invalidă!");
            }
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
                System.out.print("Introdu noul mod de tipărire (COLOR / MONOCROM): ");
                String noulMod = scanner.nextLine().toUpperCase();

                if (!noulMod.equals("COLOR") && !noulMod.equals("MONOCROM")) {
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
}

