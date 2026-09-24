package ar.edu.unlp.rescuesync.auth;

import ar.edu.unlp.rescuesync.entity.Organizacion;
import jakarta.persistence.*;

@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String apellido;

    @Column(nullable = false, unique = true)
    private String email;

    // falta hashearla
    private String password;

    @Enumerated(EnumType.STRING)
    private Rol rol;

    @ManyToOne
    private Organizacion organizacion;

    protected Usuario() {
    }

    public Usuario(
            String nombre,
            String apellido,
            String email,
            String password,
            Rol rol,
            Organizacion organizacion) {

        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.password = password;

        asignarRol(rol);
        asignarOrganizacion(organizacion);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Rol getRol() {
        return rol;
    }

    public Organizacion getOrganizacion() {
        return organizacion;
    }

    private void asignarRol(Rol rol) {

        if (rol == null) {

            throw new IllegalArgumentException("El usuario debe tener un rol");

        }

        this.rol = rol;

    }

    private void asignarOrganizacion(Organizacion organizacion) {

        if (rol == Rol.AUDITOR) {
            this.organizacion = null;
            return;
        }

        if (organizacion == null) {
            throw new IllegalArgumentException(
                    "Este rol requiere una organización"
            );
        }

        if (!rol.getTipoOrganizacion().isInstance(organizacion)) {
            throw new IllegalArgumentException(
                    "La organización no corresponde con el rol"
            );
        }

        this.organizacion = organizacion;
    }
}