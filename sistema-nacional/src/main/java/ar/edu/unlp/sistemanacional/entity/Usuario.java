package ar.edu.unlp.sistemanacional.entity;

import ar.edu.unlp.sistemanacional.entity.enums.Rol;
import jakarta.persistence.*;

// Cuenta para entrar a la API (POST /api/auth/login).
// Las de rol ONG están atadas a su ONG; ADMIN y SISTEMA no tienen ONG.
@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;   // BCrypt

    @Enumerated(EnumType.STRING)
    private Rol rol;

    @ManyToOne
    private Ong ong;

    private boolean activo = true;

    protected Usuario() {
    }

    public Usuario(String username, String passwordHash, Rol rol, Ong ong) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.ong = ong;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
    public Ong getOng() { return ong; }
    public boolean isActivo() { return activo; }
}
