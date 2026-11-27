package ioniq6.cpclimate;

/** Called under the consumer's registration lock. Ownership is not native activation. */
final class ThemeOwnership {
    private Object owner;
    private boolean confirmed;
    Object owner(){return owner;}
    boolean confirmed(Object provider){return provider!=null && owner==provider && confirmed;}
    void begin(Object provider){if(provider==null || owner!=null)throw new IllegalStateException("theme owner must be withdrawn first");owner=provider;confirmed=false;}
    void confirm(){if(owner==null)throw new IllegalStateException("no theme owner");confirmed=true;}
    void uncertain(){confirmed=false;}
    void clear(){owner=null;confirmed=false;}
}
