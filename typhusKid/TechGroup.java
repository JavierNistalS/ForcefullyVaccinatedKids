package typhusKid;
import aic2021.user.*;

public class TechGroup {
    Technology[] techs;
    int researched;

    TechGroup(Technology... techs)
    {
        this.techs = techs;
        researched = 0;
    }
}
