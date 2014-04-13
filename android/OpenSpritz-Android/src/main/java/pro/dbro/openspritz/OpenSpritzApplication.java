package pro.dbro.openspritz;

import com.squareup.otto.Bus;

/**
 * A custom application that sets up common functionality.
 *
 * @author defer (diogo@underdev.org)
 */
public interface OpenSpritzApplication {

    /**
     * Obtains the Bus that is used throughout the App.
     *
     * @return The bus instance used throughout the app.
     */
    public Bus getBus();
}
