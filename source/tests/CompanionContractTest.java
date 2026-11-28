package ioniq6.cpclimate;
import hmgapi.mods.wireless.api.CarPlayIdentification;
public final class CompanionContractTest {
    public static void main(String[] args) throws Exception {
        if(CarPlayIdentification.API_VERSION!=3 || !"carplay-identification".equals(CarPlayIdentification.SERVICE))
            throw new AssertionError("provider version/service");
        ThemeNegotiation.verifyContract(CarPlayIdentification.class,CarPlayIdentification.ThemeIdentity.class);
        CarPlayIdentification.ThemeIdentity theme=new CarPlayIdentification.ThemeIdentity("44C9FA31EC",985L,"19.0");
        if(!"44C9FA31EC:985:19.0".equals(theme.encoded())) throw new AssertionError("theme tuple");
        CarPlayIdentification.VehicleIdentity gas=new CarPlayIdentification.VehicleIdentity("Kia K5",
                CarPlayIdentification.VehicleIdentity.ENGINE_GASOLINE,new CarPlayIdentification.Connector[0]);
        if(gas.engineType!=0 || gas.connectors.length!=0) throw new AssertionError("gas profile");
        System.out.println("Pinned companion API contract passed");
    }
}
