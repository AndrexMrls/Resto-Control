package com.mycompany.restocontrol.view;

import com.mycompany.restocontrol.model.Pedido;
import com.restocontrol.model.PedidoKDS;

public class PedidoFrame extends com.restocontrol.ui.PedidoFrame {

    public PedidoFrame(Pedido pedido, Runnable onActualizado) {
        super(pedido, onActualizado);
    }

    public PedidoFrame(PedidoKDS pedidoKds, Runnable onActualizado) {
        super(pedidoKds, onActualizado);
    }
}
