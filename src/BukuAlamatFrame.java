
import java.awt.Image;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.*;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BukuAlamatFrame extends javax.swing.JFrame {

    static Object getInstance() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    // List untuk menyimpan semua kontak
    private ArrayList<Kontak> daftarKontak = new ArrayList<>();
    private String selectedFotoPath = ""; // Variabel untuk menyimpan path foto
    private int id; // Variabel untuk menyimpan ID kontak yang sedang diedit

    /**
     * Creates new form BukuAlamatFrame
     */
    public BukuAlamatFrame() {
        initComponents(); // Inisialisasi komponen GUI
        setupTable(); // Mengatur tabel untuk menampilkan data
        loadKontak(); // Memuat data dari database ke tabel

        // Event untuk tombol Tambah
        btnTambah.addActionListener(evt -> {
            String nama = txtNama.getText();
            String telepon = txtTelepon.getText();
            String email = txtEmail.getText();
            String alamat = txtAreaAlamat.getText();

            // Validasi field
            if (nama.isEmpty() || telepon.isEmpty() || email.isEmpty() || alamat.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Semua field harus diisi!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Validasi foto
            if (selectedFotoPath.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Harap unggah foto untuk kontak!", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Tambahkan kontak ke database
            tambahKontak(nama, telepon, email, alamat, selectedFotoPath);
            loadKontak(); // Perbarui tabel
            clearForm(); // Bersihkan form
        });

        btnEdit.addActionListener(evt -> {
    // Pastikan ada baris yang dipilih
    int selectedRow = tblKontak.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Pilih kontak yang ingin diedit!", "Error", JOptionPane.WARNING_MESSAGE);
        return;
    }

    try {
        // Ambil data dari tabel
        int id = (int) tblKontak.getValueAt(selectedRow, 0);        // Kolom ID
        String nama = (String) tblKontak.getValueAt(selectedRow, 1); // Kolom Nama
        String telepon = (String) tblKontak.getValueAt(selectedRow, 2); // Kolom No Telepon
        String email = (String) tblKontak.getValueAt(selectedRow, 3); // Kolom Email
        String alamat = (String) tblKontak.getValueAt(selectedRow, 4); // Kolom Alamat
        String foto = (String) tblKontak.getValueAt(selectedRow, 5); // Kolom Foto

        // Masukkan data ke form input
        txtNama.setText(nama);
        txtTelepon.setText(telepon);
        txtEmail.setText(email);
        txtAreaAlamat.setText(alamat);

        // Tampilkan foto jika ada
        if (foto != null && !foto.isEmpty()) {
            selectedFotoPath = foto;
            ImageIcon imageIcon = new ImageIcon(foto);
            Image scaledImage = imageIcon.getImage().getScaledInstance(lblFoto.getWidth(), lblFoto.getHeight(), Image.SCALE_SMOOTH);
            lblFoto.setIcon(new ImageIcon(scaledImage));
            lblFoto.setText("");
        } else {
            lblFoto.setIcon(null);
            lblFoto.setText("Foto tidak tersedia");
        }

        // Simpan ID kontak yang dipilih untuk proses update berikutnya
        this.id = id;

        // Aktifkan tombol Simpan
        btnSimpan.setEnabled(true);

       
    } catch (Exception e) {
        // Tampilkan error jika terjadi kesalahan
        JOptionPane.showMessageDialog(this, "Terjadi kesalahan: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }



            // Aktifkan tombol Simpan
            btnSimpan.setEnabled(true);

           // Tambahkan aksi pada tombol Simpan
            btnSimpan.addActionListener(saveEvent -> {
                // Validasi input
                if (txtNama.getText().trim().isEmpty() || 
                    txtTelepon.getText().trim().isEmpty() || 
                    txtEmail.getText().trim().isEmpty() || 
                    txtAreaAlamat.getText().trim().isEmpty()) {

                    JOptionPane.showMessageDialog(this, "Semua field harus diisi!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    // Perbarui kontak di database
                    updateKontak(id, txtNama.getText().trim(), 
                                      txtTelepon.getText().trim(), 
                                      txtEmail.getText().trim(), 
                                      txtAreaAlamat.getText().trim(), 
                                      selectedFotoPath);

                    // Refresh tabel
                    loadKontak();

                    // Bersihkan form input
                    clearForm();

                    // Nonaktifkan tombol Simpan
                    btnSimpan.setEnabled(false);

                    // Tampilkan pesan sukses
                    JOptionPane.showMessageDialog(this, "Kontak berhasil diperbarui!");
                } catch (Exception e) {
                    // Tampilkan pesan error jika terjadi kesalahan saat memperbarui kontak
                    JOptionPane.showMessageDialog(this, 
                                                  "Terjadi kesalahan saat memperbarui kontak: " + e.getMessage(), 
                                                  "Error", 
                                                  JOptionPane.ERROR_MESSAGE);
                }
            });

        });

        btnHapus.addActionListener(evt -> {
            int selectedRow = tblKontak.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Pilih kontak yang ingin dihapus!", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Ambil ID dari kolom pertama di tabel
            int id = (int) tblKontak.getValueAt(selectedRow, 0);

            // Hapus kontak dari database
            hapusKontak(id);

            // Perbarui tabel
            loadKontak();
        });

        

        btnCari.addActionListener(evt -> {
            String namaDicari = JOptionPane.showInputDialog(this, "Masukkan Nama yang Ingin Dicari:");

            if (namaDicari == null || namaDicari.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nama tidak boleh kosong!", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Ambil detail kontak berdasarkan nama
            Map<String, String> detailKontak = getDetailKontakByNama(namaDicari);

            if (detailKontak == null || detailKontak.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Kontak dengan nama \"" + namaDicari + "\" tidak ditemukan.", "Pencarian Gagal", JOptionPane.INFORMATION_MESSAGE);
            } else {
                new DetailViewFrame(detailKontak).setVisible(true);
            }
        });

        // Event untuk tombol Upload Foto
        btnUploadFoto.addActionListener(evt -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Pilih Foto");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            // Filter untuk hanya menerima file gambar
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Gambar (JPG, PNG)", "jpg", "png"));

            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                selectedFotoPath = selectedFile.getAbsolutePath(); // Simpan path file

                // Tampilkan gambar ke dalam JLabel
                ImageIcon imageIcon = new ImageIcon(selectedFotoPath);
                Image scaledImage = imageIcon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                lblFoto.setIcon(new ImageIcon(scaledImage));
                lblFoto.setText(""); // Kosongkan teks jika foto ada
            }
        });

        btnExportToTxt.addActionListener(evt -> {
            exportToTxt();
        });

    }
    
    
    
    private void exportToTxt() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Simpan File TXT");
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

    int result = fileChooser.showSaveDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();

        // Pastikan file memiliki ekstensi .txt
        if (!file.getAbsolutePath().endsWith(".txt")) {
            file = new File(file.getAbsolutePath() + ".txt");
        }

        try (FileWriter fw = new FileWriter(file);
             BufferedWriter bw = new BufferedWriter(fw)) {

            DefaultTableModel model = (DefaultTableModel) tblKontak.getModel();

            // Tulis header kolom
            for (int i = 0; i < model.getColumnCount(); i++) {
                bw.write(model.getColumnName(i)); // Tulis nama kolom
                if (i < model.getColumnCount() - 1) bw.write("\t"); // Pisahkan dengan tab
            }
            bw.newLine();

            // Tulis data baris
            for (int i = 0; i < model.getRowCount(); i++) {
                for (int j = 0; j < model.getColumnCount(); j++) {
                    bw.write(String.valueOf(model.getValueAt(i, j))); // Tulis nilai sel
                    if (j < model.getColumnCount() - 1) bw.write("\t"); // Pisahkan dengan tab
                }
                bw.newLine();
            }

            JOptionPane.showMessageDialog(this, "Data berhasil diekspor ke file: " + file.getAbsolutePath(), "Sukses", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal menyimpan file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}




    private Map<String, String> getDetailKontakByNama(String nama) {
        Map<String, String> detail = new LinkedHashMap<>(); // Inisialisasi map kosong

        String query = "SELECT * FROM kontak WHERE nama = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, nama);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                detail.put("Nama", rs.getString("nama"));
                detail.put("No Telepon", rs.getString("telepon"));
                detail.put("Email", rs.getString("email"));
                detail.put("Alamat", rs.getString("alamat"));
                detail.put("Foto", rs.getString("foto")); // Pastikan kolom 'foto' ada di tabel
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal mengambil data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        return detail; // Kembalikan map kosong jika tidak ada data
    }

    private List<Object[]> cariKontakBerdasarkanNama(String nama) {
        List<Object[]> hasil = new ArrayList<>();
        String query = "SELECT * FROM kontak WHERE nama LIKE ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, "%" + nama + "%"); // Gunakan wildcard untuk pencarian nama
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                // Tambahkan setiap baris hasil pencarian ke list
                hasil.add(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nama"),
                    rs.getString("telepon"),
                    rs.getString("email"),
                    rs.getString("alamat")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal mencari data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        return hasil;
    }

    private void tambahKontak(String nama, String telepon, String email, String alamat, String fotoPath) {
        String query = "INSERT INTO kontak (nama, telepon, email, alamat, foto) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, nama);
            stmt.setString(2, telepon);
            stmt.setString(3, email);
            stmt.setString(4, alamat);
            stmt.setString(5, fotoPath);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Kontak berhasil ditambahkan!");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal menambahkan kontak: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadKontak() {
        String query = "SELECT * FROM kontak";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query);
                ResultSet rs = stmt.executeQuery()) {

            DefaultTableModel model = (DefaultTableModel) tblKontak.getModel();
            model.setRowCount(0); // Hapus data lama di tabel

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nama"),
                    rs.getString("telepon"),
                    rs.getString("email"),
                    rs.getString("alamat"),
                    rs.getString("foto") // Tambahkan path foto jika diperlukan
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateKontak(int id, String nama, String telepon, String email, String alamat, String fotoPath) {
        String query = "UPDATE kontak SET nama = ?, telepon = ?, email = ?, alamat = ?, foto = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, nama);
            stmt.setString(2, telepon);
            stmt.setString(3, email);
            stmt.setString(4, alamat);
            stmt.setString(5, fotoPath);
            stmt.setInt(6, id);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memperbarui kontak: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void hapusKontak(int id) {
        String query = "DELETE FROM kontak WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "Kontak berhasil dihapus!");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal menghapus kontak: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Mengatur tabel dengan kolom "Nama", "No Telepon", "Email", "Alamat"
    private void setupTable() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Nama", "No Telepon", "Email", "Alamat", "Foto"}, 0
        );
        tblKontak.setModel(model); // Sambungkan model ke tabel
    }

    // Memperbarui tabel dengan data dari daftar kontak
    private void updateTabel() {
        DefaultTableModel model = (DefaultTableModel) tblKontak.getModel();
        model.setRowCount(0); // Hapus semua data lama di tabel

        // Tambahkan setiap kontak dari daftar ke tabel
        for (Kontak kontak : daftarKontak) {
            model.addRow(new Object[]{kontak.getNama(), kontak.getTelepon(), kontak.getEmail(), kontak.getAlamat()});
        }
    }

    // Membersihkan input form setelah data disimpan
    private void clearForm() {
        txtNama.setText("");
        txtTelepon.setText("");
        txtEmail.setText("");
        txtAreaAlamat.setText("");
        lblFoto.setIcon(null); // Bersihkan label foto
        lblFoto.setText(""); // Tampilkan teks default
        selectedFotoPath = ""; // Kosongkan path foto
    }

    // Menyimpan daftar kontak ke file menggunakan ObjectOutputStream
    private void saveToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("data_kontak.dat"))) {
            oos.writeObject(daftarKontak); // Simpan objek daftarKontak ke file
            JOptionPane.showMessageDialog(this, "Data berhasil disimpan!");
        } catch (IOException e) {
            e.printStackTrace(); // Tangani kesalahan jika terjadi
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        txtNama = new javax.swing.JTextField();
        txtTelepon = new javax.swing.JTextField();
        txtEmail = new javax.swing.JTextField();
        btnTambah = new javax.swing.JButton();
        btnEdit = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        btnCari = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblKontak = new javax.swing.JTable();
        btnSimpan = new javax.swing.JButton();
        btnUploadFoto = new javax.swing.JButton();
        lblFoto = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtAreaAlamat = new javax.swing.JTextArea();
        btnExportToTxt = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(238, 206, 185));
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED), "Aplikasi Buku Alamat", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Times New Roman", 1, 36), new java.awt.Color(102, 0, 102))); // NOI18N

        txtNama.setBackground(new java.awt.Color(254, 251, 216));
        txtNama.setBorder(javax.swing.BorderFactory.createTitledBorder("Nama"));

        txtTelepon.setBackground(new java.awt.Color(254, 251, 216));
        txtTelepon.setBorder(javax.swing.BorderFactory.createTitledBorder("No Telpon"));
        txtTelepon.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtTeleponKeyTyped(evt);
            }
        });

        txtEmail.setBackground(new java.awt.Color(254, 251, 216));
        txtEmail.setBorder(javax.swing.BorderFactory.createTitledBorder("Email"));

        btnTambah.setBackground(new java.awt.Color(187, 154, 177));
        btnTambah.setText("Tambah");
        btnTambah.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnTambah.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        btnEdit.setBackground(new java.awt.Color(187, 154, 177));
        btnEdit.setText("Edit");
        btnEdit.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnEdit.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditActionPerformed(evt);
            }
        });

        btnHapus.setBackground(new java.awt.Color(187, 154, 177));
        btnHapus.setText("Hapus");
        btnHapus.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnHapus.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        btnCari.setBackground(new java.awt.Color(187, 154, 177));
        btnCari.setText("Cari");
        btnCari.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnCari.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        tblKontak.setBackground(new java.awt.Color(254, 251, 216));
        tblKontak.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        tblKontak.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Nama", "No Telpon", "Email", "Alamat"
            }
        ));
        tblKontak.setGridColor(new java.awt.Color(51, 51, 51));
        tblKontak.setName(""); // NOI18N
        tblKontak.setSelectionBackground(new java.awt.Color(217, 22, 86));
        tblKontak.setSelectionForeground(new java.awt.Color(0, 0, 0));
        jScrollPane1.setViewportView(tblKontak);

        btnSimpan.setBackground(new java.awt.Color(187, 154, 177));
        btnSimpan.setText("Simpan");
        btnSimpan.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnSimpan.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        btnUploadFoto.setBackground(new java.awt.Color(187, 154, 177));
        btnUploadFoto.setText("Upload Foto");
        btnUploadFoto.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnUploadFoto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUploadFotoActionPerformed(evt);
            }
        });

        lblFoto.setBackground(new java.awt.Color(152, 125, 154));
        lblFoto.setForeground(new java.awt.Color(152, 125, 154));
        lblFoto.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        txtAreaAlamat.setBackground(new java.awt.Color(254, 251, 216));
        txtAreaAlamat.setColumns(20);
        txtAreaAlamat.setRows(5);
        txtAreaAlamat.setBorder(javax.swing.BorderFactory.createTitledBorder("Alamat"));
        jScrollPane2.setViewportView(txtAreaAlamat);

        btnExportToTxt.setText("Export Data");
        btnExportToTxt.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(22, 22, 22)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(lblFoto, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(btnUploadFoto, javax.swing.GroupLayout.DEFAULT_SIZE, 167, Short.MAX_VALUE))
                                        .addGap(18, 18, 18)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                            .addComponent(txtTelepon)
                                            .addComponent(txtNama, javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(txtEmail)
                                            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGap(22, 22, 22)
                                        .addComponent(btnTambah, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(btnEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(btnHapus, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(btnSimpan, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(125, 125, 125)
                                .addComponent(btnCari, javax.swing.GroupLayout.PREFERRED_SIZE, 224, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnExportToTxt)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(52, 52, 52)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(lblFoto, javax.swing.GroupLayout.PREFERRED_SIZE, 189, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(29, 29, 29)
                        .addComponent(btnUploadFoto))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(txtNama, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtTelepon, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(36, 36, 36)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnTambah)
                    .addComponent(btnEdit)
                    .addComponent(btnHapus)
                    .addComponent(btnSimpan))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCari, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 31, Short.MAX_VALUE)
                .addComponent(btnExportToTxt)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtTeleponKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtTeleponKeyTyped
        // Validasi agar hanya angka yang bisa dimasukkan
        char c = evt.getKeyChar();
        if (!Character.isDigit(c)) {
            evt.consume(); // Abaikan karakter yang bukan angka
            JOptionPane.showMessageDialog(null, "Hanya angka yang diperbolehkan!", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }

        // Batasi panjang input maksimal 13 karakter
        if (txtTelepon.getText().length() >= 13) {
            evt.consume(); // Abaikan input jika panjang lebih dari 13
            JOptionPane.showMessageDialog(null, "Maksimal 13 angka!", "Invalid Input", JOptionPane.WARNING_MESSAGE);
        }
        // TODO add your handling code here:
    }//GEN-LAST:event_txtTeleponKeyTyped

    private void btnUploadFotoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUploadFotoActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Pilih Foto");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // Filter untuk hanya menerima file gambar
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Gambar (JPG, PNG)", "jpg", "png"));

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();

            // Tampilkan gambar ke dalam JLabel
            ImageIcon imageIcon = new ImageIcon(selectedFile.getAbsolutePath());
            Image scaledImage = imageIcon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH); // Sesuaikan ukuran
            lblFoto.setIcon(new ImageIcon(scaledImage));
        }        // TODO add your handling code here:
    }//GEN-LAST:event_btnUploadFotoActionPerformed

    private void btnEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnEditActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        SwingUtilities.invokeLater(() -> {
        LoginFrame loginFrame = new LoginFrame();
        loginFrame.setVisible(true); // Tampilkan halaman login saat aplikasi dijalankan
    }); // Jalankan aplikasi
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCari;
    private javax.swing.JButton btnEdit;
    private javax.swing.JButton btnExportToTxt;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnSimpan;
    private javax.swing.JButton btnTambah;
    private javax.swing.JButton btnUploadFoto;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblFoto;
    private javax.swing.JTable tblKontak;
    private javax.swing.JTextArea txtAreaAlamat;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextField txtNama;
    private javax.swing.JTextField txtTelepon;
    // End of variables declaration//GEN-END:variables
}
