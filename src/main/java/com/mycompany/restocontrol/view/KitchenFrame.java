package com.mycompany.restocontrol.view;

import com.mycompany.restocontrol.controller.RestaurantController;

public final class KitchenFrame extends com.restocontrol.ui.KitchenFrame {

    private static final long serialVersionUID = 1L;

    public KitchenFrame(RestaurantController controller) {
        super(controller);
    }

    public KitchenFrame() {
        super(null);
    }
}
