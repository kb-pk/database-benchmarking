package bench.app.model.common;

/// models that have a one-to-one bidirectional reference (e.g. bookshop.manager and employee.) cant be instantiated
/// at once, since it'd be infinitely recursive
@FunctionalInterface
public interface LazilyInstantiated<InstantiatedModel, Ref> {
    InstantiatedModel instantiateWith(Ref reference);
}
