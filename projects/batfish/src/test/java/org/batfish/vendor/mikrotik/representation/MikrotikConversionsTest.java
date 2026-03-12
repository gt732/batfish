package org.batfish.vendor.mikrotik.representation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceType;
import org.junit.Test;

public final class MikrotikConversionsTest {

  @Test
  public void testToViInterfaceEthernetWithAddress() {
    MikrotikInterface iface = new MikrotikInterface("ether1", "ether");
    iface.setDisabled(false);
    ConcreteInterfaceAddress address = ConcreteInterfaceAddress.parse("192.0.2.1/24");
    iface.addAddress(address);

    Interface viIface = Conversions.toViInterface(iface);

    assertThat(viIface.getName(), equalTo("ether1"));
    assertThat(viIface.getInterfaceType(), equalTo(InterfaceType.PHYSICAL));
    assertThat(viIface.getAdminUp(), equalTo(true));
    assertThat(viIface.getAddress(), equalTo(address));
  }

  @Test
  public void testToViInterfaceDisabled() {
    MikrotikInterface iface = new MikrotikInterface("ether2", "ether");
    iface.setDisabled(true);

    Interface viIface = Conversions.toViInterface(iface);

    assertThat(viIface.getAdminUp(), equalTo(false));
  }

  @Test
  public void testToViInterfaceBridgeType() {
    MikrotikInterface iface = new MikrotikInterface("bridge1", "bridge");

    Interface viIface = Conversions.toViInterface(iface);

    assertThat(viIface.getInterfaceType(), equalTo(InterfaceType.PHYSICAL));
  }

  @Test
  public void testToViInterfaceNoAddress() {
    MikrotikInterface iface = new MikrotikInterface("ether3", "ether");

    Interface viIface = Conversions.toViInterface(iface);

    assertThat(viIface.getAddress(), nullValue());
  }

  @Test
  public void testToViInterfaceDeterministic() {
    MikrotikInterface iface = new MikrotikInterface("loopback1", "loopback");
    ConcreteInterfaceAddress address = ConcreteInterfaceAddress.parse("198.51.100.1/32");
    iface.addAddress(address);

    Interface viIface1 = Conversions.toViInterface(iface);
    Interface viIface2 = Conversions.toViInterface(iface);
    Interface viIface3 = Conversions.toViInterface(iface);

    assertThat(viIface1.getName(), equalTo(viIface2.getName()));
    assertThat(viIface2.getName(), equalTo(viIface3.getName()));
    assertThat(viIface1.getInterfaceType(), equalTo(viIface2.getInterfaceType()));
    assertThat(viIface2.getInterfaceType(), equalTo(viIface3.getInterfaceType()));
    assertThat(viIface1.getAddress(), equalTo(viIface2.getAddress()));
    assertThat(viIface2.getAddress(), equalTo(viIface3.getAddress()));
    assertThat(viIface1.getAdminUp(), equalTo(viIface2.getAdminUp()));
    assertThat(viIface2.getAdminUp(), equalTo(viIface3.getAdminUp()));
  }
}
