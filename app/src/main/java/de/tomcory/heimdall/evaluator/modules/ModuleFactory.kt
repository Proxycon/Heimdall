package de.tomcory.heimdall.evaluator.modules

/**
 * Module factory, solely responsible for instantiating metric modules.
 * This is the single point of manual registry to introduce new module into the system.
 * The Evaluator fetches registered modules from here.
 */
object ModuleFactory {

    /**
     * List of registered modules get populated in init
     */
    val registeredModules = mutableListOf<Module>()

    // instantiates manually added modules
    init {
        this.registeredModules.add(StaticPermissionsScore())
        this.registeredModules.add(TrackerScore())
        /* add new modules here:
        * this.registeredModules.add(newModule())
        */
    }
}
