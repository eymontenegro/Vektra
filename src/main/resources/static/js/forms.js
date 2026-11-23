/*
 * Comportamiento común de los formularios de Vektra.
 *
 *  - Formularios con data-validate: muestran en español los mensajes de
 *    validación del navegador (required, pattern, minlength, min/max...).
 *    Los textos salen de los atributos data-message-* de cada campo.
 *    El servidor vuelve a validar todo: esto solo ayuda a la persona.
 *  - Botón con data-busy-text: se desactiva al enviar para evitar compras o
 *    registros repetidos por doble clic.
 *  - Formularios con data-confirm (y data-confirm-again): piden confirmación
 *    antes de enviar, igual que la doble confirmación de DeleteDriverPanel.
 *  - Selector con data-toggle-register: en el login oculta "Crear cuenta"
 *    cuando se elige Administrador.
 *  - Botón con data-dismiss="alert": cierra el mensaje de éxito o error.
 *  - Selector con data-line-select: pinta el punto de color de la línea
 *    elegida (formulario de vehículo). El color sale de data-color.
 */
(function () {
    'use strict';

    /** Devuelve el mensaje en español para el primer error del campo. */
    function messageFor(field) {
        var validity = field.validity;
        var data = field.dataset;
        if (validity.valueMissing) {
            return data.messageRequired || 'Este campo es obligatorio.';
        }
        if (validity.typeMismatch) {
            return data.messageType || 'El formato no es válido.';
        }
        if (validity.patternMismatch) {
            return data.messagePattern || 'El formato no es válido.';
        }
        if (validity.tooShort) {
            return data.messageShort || 'El texto es demasiado corto.';
        }
        if (validity.rangeUnderflow || validity.rangeOverflow || validity.stepMismatch) {
            return data.messageRange || 'El valor está fuera del rango permitido.';
        }
        return '';
    }

    /** Pinta el campo en rojo o verde, como hacía Styles.markField. */
    function markField(field) {
        var valid = field.checkValidity();
        field.classList.toggle('is-invalid', !valid);
        field.classList.toggle('is-valid', valid && field.value !== '');
    }

    function setUpValidation(form) {
        var fields = form.querySelectorAll('input, select, textarea');
        fields.forEach(function (field) {
            field.addEventListener('invalid', function () {
                field.setCustomValidity(messageFor(field));
                markField(field);
            });
            field.addEventListener('input', function () {
                field.setCustomValidity('');
                if (field.classList.contains('is-invalid')) {
                    markField(field);
                }
            });
            field.addEventListener('change', function () {
                field.setCustomValidity('');
                markField(field);
            });
        });
    }

    function setUpSubmit(form) {
        form.addEventListener('submit', function (event) {
            if (form.dataset.confirm && !window.confirm(form.dataset.confirm)) {
                event.preventDefault();
                return;
            }
            if (form.dataset.confirmAgain && !window.confirm(form.dataset.confirmAgain)) {
                event.preventDefault();
                return;
            }
            var button = form.querySelector('button[data-busy-text]');
            if (button) {
                button.disabled = true;
                button.textContent = button.dataset.busyText;
            }
        });
    }

    function setUpRegisterToggle() {
        var select = document.querySelector('[data-toggle-register]');
        var hint = document.getElementById('registerHint');
        if (!select || !hint) {
            return;
        }
        var update = function () {
            hint.hidden = select.value === 'ADMIN';
        };
        select.addEventListener('change', update);
        update();
    }

    function setUpAlertClose() {
        document.querySelectorAll('[data-dismiss="alert"]').forEach(function (button) {
            button.addEventListener('click', function () {
                var alert = button.closest('.alert');
                if (alert) {
                    alert.remove();
                }
            });
        });
    }

    function setUpLineDot() {
        var select = document.querySelector('[data-line-select]');
        var dot = document.getElementById('lineDot');
        if (!select || !dot) {
            return;
        }
        var update = function () {
            var option = select.options[select.selectedIndex];
            dot.style.background = option ? option.dataset.color : '';
        };
        select.addEventListener('change', update);
        update();
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('form[data-validate]').forEach(setUpValidation);
        document.querySelectorAll('form').forEach(setUpSubmit);
        setUpRegisterToggle();
        setUpAlertClose();
        setUpLineDot();
    });
})();
