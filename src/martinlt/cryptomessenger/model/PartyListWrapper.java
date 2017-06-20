package martinlt.cryptomessenger.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Helper class to wrap a list of parties. This is used for saving the
 * list of parties to XML.
 */
@XmlRootElement(name = "parties")
public class PartyListWrapper {

    private List<Party> partys;

    @XmlElement(name = "party")
    public List<Party> getPartys() {
        return partys;
    }

    public void setPartys(List<Party> partys) {
        this.partys = partys;
    }
}