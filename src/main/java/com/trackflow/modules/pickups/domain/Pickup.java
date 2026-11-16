package com.trackflow.modules.pickups.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Recogida a domicilio: la visita programada para buscar un paquete en la dirección
 * del remitente.
 *
 * Es un agregado propio, aparte del envío. La razón es que la recogida se puede pedir
 * antes de que exista el envío (se recoge el paquete y luego se registra), y porque su
 * ciclo de vida es otro: se solicita, se programa, va en camino y se recoge, mientras
 * el envío recorre la red.
 *
 * Los datos privados (dirección, nombre y teléfono de contacto) viven aquí y no salen
 * de este módulo: quien consulta una recogida que no tiene asignada los recibe
 * vacíos, y el resto de APIs solo se enteran de que hubo una recogida, nunca de dónde.
 */
@Entity
@Table(name = "pickups")
public class Pickup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private PickupNumber pickupNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PickupStatus status;

    @Column(nullable = false)
    private String address;

    @Column(name = "city_id", nullable = false)
    private Long cityId;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(name = "contact_phone", nullable = false)
    private String contactPhone;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "time_slot", nullable = false)
    private String timeSlot;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_size", nullable = false)
    private PackageSize packageSize;

    @Column(name = "weight_kg", nullable = false, precision = 8, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "shipment_tracking_number")
    private String shipmentTrackingNumber;

    @Column(name = "assigned_username", length = 50)
    private String assignedUsername;

    @Column(name = "requested_by", nullable = false, length = 50)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Pickup() {
    }

    private Pickup(PickupNumber pickupNumber, String address, Long cityId, String contactName, String contactPhone,
            LocalDate scheduledDate, String timeSlot, PackageSize packageSize, BigDecimal weightKg,
            String shipmentTrackingNumber, String assignedUsername, String requestedBy, Instant requestedAt) {
        this.pickupNumber = pickupNumber;
        this.address = address;
        this.cityId = cityId;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.scheduledDate = scheduledDate;
        this.timeSlot = timeSlot;
        this.packageSize = packageSize;
        this.weightKg = weightKg;
        this.shipmentTrackingNumber = shipmentTrackingNumber;
        this.assignedUsername = assignedUsername;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
        this.updatedAt = requestedAt;
        this.status = PickupStatus.REQUESTED;
    }

    public static Pickup registrar(PickupNumber pickupNumber, String address, Long cityId, String contactName,
            String contactPhone, LocalDate scheduledDate, String timeSlot, PackageSize packageSize, BigDecimal weightKg,
            String shipmentTrackingNumber, String assignedUsername, String requestedBy, Instant requestedAt) {
        return new Pickup(pickupNumber, address, cityId, contactName, contactPhone, scheduledDate, timeSlot,
                packageSize, weightKg, shipmentTrackingNumber, assignedUsername, requestedBy, requestedAt);
    }

    /**
     * Cambia la fecha o la franja y, si se indica un responsable, lo asigna.
     *
     * Asignar es lo que convierte una solicitud en una recogida programada: mientras
     * nadie la tenga asignada es solo un deseo, y no se le manda a nadie a ninguna
     * parte. Por eso asignar y reprogramar pueden ser la misma operación.
     *
     * @return el tipo de cambio que se debe registrar en la bitácora
     */
    public PickupEventType reprogramar(LocalDate fecha, String franja, String responsable, Instant ahora) {
        if (!status.admiteReprogramacion()) {
            throw new TransicionDeRecogidaInvalidaException(pickupNumber.value(), status, "reprogramar");
        }

        this.scheduledDate = fecha;
        this.timeSlot = franja;
        this.updatedAt = ahora;

        if (responsable != null && !responsable.isBlank() && !responsable.equals(this.assignedUsername)) {
            this.assignedUsername = responsable;
            this.status = PickupStatus.SCHEDULED;
            return PickupEventType.ASSIGNED;
        }

        return PickupEventType.SCHEDULED;
    }

    /** Cancela la recogida. Solo se puede mientras nadie haya salido a recoger. */
    public void cancelar(Instant ahora) {
        if (!status.admiteCancelacion()) {
            throw new TransicionDeRecogidaInvalidaException(pickupNumber.value(), status, "cancelar");
        }
        this.status = PickupStatus.CANCELLED;
        this.updatedAt = ahora;
    }

    /** El operador salió hacia la dirección del remitente. */
    public void despachar(Instant ahora) {
        if (!status.admiteDespacho()) {
            throw new TransicionDeRecogidaInvalidaException(pickupNumber.value(), status, "despachar");
        }
        this.status = PickupStatus.ON_THE_WAY;
        this.updatedAt = ahora;
    }

    /** El paquete ya está en manos de la empresa. */
    public void completar(Instant ahora) {
        if (!status.admiteCompletado()) {
            throw new TransicionDeRecogidaInvalidaException(pickupNumber.value(), status, "completar");
        }
        this.status = PickupStatus.PICKED_UP;
        this.updatedAt = ahora;
    }

    /**
     * Vincula la recogida con un envío.
     *
     * Se puede vincular una recogida ya recogida: en ese caso el envío entra en
     * custodia en el momento de la vinculación, no con la fecha de la recogida, porque
     * un movimiento anterior al registro del envío no sería válido.
     */
    public void vincularEnvio(String trackingNumber, Instant ahora) {
        if (!status.admiteVinculacion()) {
            throw new TransicionDeRecogidaInvalidaException(pickupNumber.value(), status, "vincular un envío");
        }
        if (shipmentTrackingNumber != null && !shipmentTrackingNumber.equals(trackingNumber)) {
            throw new IllegalStateException("La recogida ya está vinculada a otro envío");
        }
        this.shipmentTrackingNumber = trackingNumber;
        this.updatedAt = ahora;
    }

    /**
     * Si quien consulta tiene la recogida asignada puede ver la dirección y el
     * contacto; si no, no.
     *
     * No hay excepción para el administrador: la regla es de la asignación, no del
     * rol. Un administrador que necesite ver la dirección se asigna la recogida, que
     * para eso puede hacerlo.
     *
     * @param solicitante el usuario que consulta, o null si no hay sesión abierta
     */
    public boolean puedeVerDatosPrivados(String solicitante) {
        return solicitante != null && solicitante.equals(assignedUsername);
    }

    public Long getId() {
        return id;
    }

    public PickupNumber getPickupNumber() {
        return pickupNumber;
    }

    public PickupStatus getStatus() {
        return status;
    }

    public String getAddress() {
        return address;
    }

    public Long getCityId() {
        return cityId;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public PackageSize getPackageSize() {
        return packageSize;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public String getShipmentTrackingNumber() {
        return shipmentTrackingNumber;
    }

    public String getAssignedUsername() {
        return assignedUsername;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
