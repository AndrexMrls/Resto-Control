package com.mycompany.restocontrol.view;

import com.mycompany.restocontrol.model.Pedido;
import com.restocontrol.model.PedidoKDS;

public class CobrarFrame extends com.restocontrol.ui.CobrarFrame {

    public CobrarFrame(Pedido pedido, Runnable onCobrado) {
        super(pedido, onCobrado);
    }

    public CobrarFrame(PedidoKDS pedidoKds, Runnable onCobrado) {
        super(pedidoKds, onCobrado);
    }
}
