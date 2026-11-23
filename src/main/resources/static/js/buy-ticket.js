/*
* Página "Comprar ticket": cuando se eligen origen y destino, muestra la
* ruta directa (tiempo, distancia, línea), igual que BuyTicketPanel.
* El punto de color toma el color de la línea de la ruta.
* Si no hay ruta directa, desactiva el botón de compra (regla del equipo;
* el servidor también la exige, así que no depende de este script).
* Consulta /api/routes/find y cancela la consulta anterior si la persona
* cambia la selección antes de que llegue la respuesta.
*/
(function () {
'use strict';
var DEFAULT_TEXT = 'Elige la estación de origen y la de destino.';
document.addEventListener('DOMContentLoaded', function () {
var origin = document.getElementById('originId');
var destination = document.getElementById('destinationId');
var info = document.getElementById('routeInfo');
var dot = document.getElementById('routeDot');
var buyButton = document.querySelector('#buyTicketForm
button[type="submit"]');
if (!origin || !destination || !info) {
return;
}
var controller = null;
/** Habilita el botón solo cuando hay una ruta directa confirmada.
*/
function setCanBuy(canBuy) {
if (buyButton) {
buyButton.disabled = !canBuy;
}
}
/** Pinta el punto con el color de la línea, o gris si no hay
ruta. */
function setLineColor(color) {
if (dot) {
dot.style.background = color || '';
}
}
function showRouteInfo() {
if (controller) {
controller.abort();
}
setCanBuy(false);
setLineColor(null);
if (!origin.value || !destination.value) {
info.textContent = DEFAULT_TEXT;
return;
}
if (origin.value === destination.value) {
info.textContent = 'El origen y el destino deben ser
estaciones distintas.';
return;
}
info.textContent = 'Buscando la ruta…';
controller = new AbortController();
var url = '/api/routes/find?originId=' +
encodeURIComponent(origin.value)
+ '&destinationId=' +
encodeURIComponent(destination.value);
fetch(url, {signal: controller.signal, headers: {'Accept':
'application/json'}})
.then(function (response) { return response.json(); })
.then(function (data) {
info.textContent = data.description;
setLineColor(data.lineColor);
setCanBuy(data.found === true);
})
.catch(function (error) {
if (error.name !== 'AbortError') {
info.textContent = 'No se pudo consultar la
ruta.';
}
});
}
origin.addEventListener('change', showRouteInfo);
destination.addEventListener('change', showRouteInfo);
showRouteInfo();
});
})();