package com.pedidos360.ms.cupones.service;

import com.pedidos360.ms.cupones.entity.Cupon;
import com.pedidos360.ms.cupones.repository.CuponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class CuponService {

    private final CuponRepository cuponRepository;

    public CuponService(CuponRepository cuponRepository) {
        this.cuponRepository = cuponRepository;
    }

    @Transactional
    public Cupon crear(Cupon cupon) {

        if (cupon.getCodigo() == null ||
                cupon.getCodigo().isBlank()) {
            throw new IllegalArgumentException(
                    "El codigo del cupon es obligatorio"
            );
        }

        String codigo = cupon.getCodigo()
                .trim()
                .toUpperCase(Locale.ROOT);

        if (cuponRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new IllegalArgumentException(
                    "Ya existe un cupon con ese codigo"
            );
        }

        if (cupon.getPorcentajeDescuento() == null ||
                cupon.getPorcentajeDescuento()
                        .compareTo(BigDecimal.ZERO) <= 0 ||
                cupon.getPorcentajeDescuento()
                        .compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(
                    "El porcentaje debe ser mayor que 0 y menor o igual a 100"
            );
        }

        if (cupon.getFechaInicio() == null ||
                cupon.getFechaVencimiento() == null ||
                cupon.getFechaVencimiento()
                        .isBefore(cupon.getFechaInicio())) {
            throw new IllegalArgumentException(
                    "Las fechas de vigencia no son validas"
            );
        }

        if (cupon.getLimiteUsos() == null ||
                cupon.getLimiteUsos() <= 0) {
            throw new IllegalArgumentException(
                    "El limite de usos debe ser mayor que cero"
            );
        }

        cupon.setId(null);
        cupon.setCodigo(codigo);
        cupon.setUsosActuales(0);
        cupon.setActivo(true);

        return cuponRepository.save(cupon);
    }

    @Transactional(readOnly = true)
    public List<Cupon> listar() {
        return cuponRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Cupon buscarPorId(Long id) {
        return cuponRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Cupon no encontrado"
                        )
                );
    }

    @Transactional(readOnly = true)
    public Cupon validar(String codigo) {

        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException(
                    "Debes ingresar un codigo de cupon"
            );
        }

        Cupon cupon = cuponRepository
                .findByCodigoIgnoreCase(codigo.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El cupon no existe"
                        )
                );

        validarVigencia(cupon);

        return cupon;
    }

    private void validarVigencia(Cupon cupon) {

        LocalDate hoy = LocalDate.now();

        if (!Boolean.TRUE.equals(cupon.getActivo())) {
            throw new IllegalArgumentException(
                    "El cupon esta desactivado"
            );
        }

        if (cupon.getFechaInicio() == null ||
                cupon.getFechaVencimiento() == null ||
                hoy.isBefore(cupon.getFechaInicio()) ||
                hoy.isAfter(cupon.getFechaVencimiento())) {
            throw new IllegalArgumentException(
                    "El cupon no esta vigente"
            );
        }

        if (cupon.getLimiteUsos() == null ||
                cupon.getUsosActuales() == null ||
                cupon.getUsosActuales() >= cupon.getLimiteUsos()) {
            throw new IllegalArgumentException(
                    "El cupon alcanzo su limite de usos"
            );
        }
    }

    @Transactional(readOnly = true)
    public ResultadoDescuento calcularDescuento(
            String codigo,
            BigDecimal subtotal) {

        validarSubtotal(subtotal);

        Cupon cupon = validar(codigo);

        return construirResultado(cupon, subtotal);
    }

    @Transactional
    public ResultadoDescuento aplicarCupon(
            String codigo,
            BigDecimal subtotal) {

        validarSubtotal(subtotal);

        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException(
                    "Debes ingresar un codigo de cupon"
            );
        }

        Cupon cupon = cuponRepository
                .buscarPorCodigoParaActualizar(codigo.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El cupon no existe"
                        )
                );

        validarVigencia(cupon);

        ResultadoDescuento resultado =
                construirResultado(cupon, subtotal);

        cupon.setUsosActuales(cupon.getUsosActuales() + 1);

        cuponRepository.save(cupon);

        return resultado;
    }

    private void validarSubtotal(BigDecimal subtotal) {

        if (subtotal == null ||
                subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El subtotal debe ser mayor que cero"
            );
        }

        if (subtotal.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(
                    "El subtotal debe ser un monto entero en pesos chilenos"
            );
        }
    }

    private ResultadoDescuento construirResultado(
            Cupon cupon,
            BigDecimal subtotal) {

        BigDecimal descuento = subtotal
                .multiply(cupon.getPorcentajeDescuento())
                .divide(
                        new BigDecimal("100"),
                        0,
                        RoundingMode.HALF_UP
                );

        BigDecimal totalFinal = subtotal.subtract(descuento);

        return new ResultadoDescuento(
                cupon.getCodigo(),
                cupon.getPorcentajeDescuento(),
                subtotal,
                descuento,
                totalFinal
        );
    }

    @Transactional
    public Cupon desactivar(Long id) {
        Cupon cupon = buscarPorId(id);
        cupon.setActivo(false);
        return cuponRepository.save(cupon);
    }

    public record ResultadoDescuento(
            String codigo,
            BigDecimal porcentajeDescuento,
            BigDecimal subtotal,
            BigDecimal descuento,
            BigDecimal totalFinal
    ) {
    }
}