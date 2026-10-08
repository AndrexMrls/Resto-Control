# RestoControl

Aplicación de escritorio para administrar pedidos, mesas y cierres de caja de un restaurante. Está desarrollada con Java Swing y FlatLaf; Maven descarga FlatLaf automáticamente al abrir o compilar el proyecto.

## Abrir en NetBeans

1. Abre NetBeans y selecciona **File > Open Project**.
2. Elige la carpeta que contiene este `pom.xml`.
3. Espera a que NetBeans cargue el proyecto Maven.
4. Ejecuta el proyecto con **Run Project**. La clase principal es `com.mycompany.restocontrol.Main`.

También puedes ejecutarla desde una terminal con Java 17 y Maven:

```text
mvn clean package
mvn exec:java
```

## Funciones

- Panel principal con ventas del día, pedidos activos, mesas ocupadas y pedidos recientes.
- Creación y seguimiento de pedidos para 12 mesas.
- Estados de pedido: Nuevo, En preparación, Listo, Entregado, Finalizado y Cancelado. La mesa sigue ocupada mientras los clientes comen y queda disponible al finalizar el pedido.
- Vista visual de ocupación de mesas y selección de mesas fuera de servicio desde la sección **Mesas**.
- Las mesas fuera de servicio no se ofrecen al registrar pedidos; las que tienen pedidos activos no pueden deshabilitarse.
- Reportes de pedidos y ventas.
- Cierre de caja de los pedidos entregados desde el cierre anterior.
- Los pedidos, cierres y selección de mesas fuera de servicio se mantienen solo en memoria mientras la aplicación está abierta. Al cerrar y volver a abrir, la aplicación inicia vacía.

El menú ofrece comida de la costa colombiana. Los importes se muestran en pesos colombianos (COP) y los precios del catálogo no superan los $40.000 COP; puedes ajustarlos en `RestaurantController`.
