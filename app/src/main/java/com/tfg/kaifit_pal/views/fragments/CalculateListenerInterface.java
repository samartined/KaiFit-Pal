package com.tfg.kaifit_pal.views.fragments;

/**
 * This interface is used to define a callback method that will be called when a calculation is completed.
 * The method takes an integer as a parameter, which represents the result of the calculation.
 * This interface should be implemented by any class that needs to respond to this event.
 */
public interface CalculateListenerInterface {

    /**
     * This method is called when a calculation is completed.
     * @param tdeeResult The result of the calculation.
     */
    void onCalculateClick(int tdeeResult);
}