DIR="./SRF/"

  # use "-name '*.java'" instead of "-type f" if you want to match java source files only

  find "${DIR}" -name '*.java' -print | while read f; do
    sed -i -e 's/\.xmcda_3_0/.xmcda_v3/g' "$f";
    sed -i -e 's/\.xmcda_2_2_1/.xmcda_v2/g' "$f";
    sed -i -e 's/\.converters\.v2_2_1_v3_0\.XMCDAConverter/.converters.v2_v3.XMCDAConverter/g' "$f";
    sed -i -e 's/\.v2_2_1/.v2/g' "$f";
  done


  find "${DIR}" -name '*.xml' -print | while read f; do
    sed -i -e 's,http://www.decision-deck.org/2012/XMCDA-2.2.1,http://www.decision-deck.org/2016/XMCDA-2.2.2,g' "$f";    
    sed -i -e 's,http://www.decision-deck.org/xmcda/_downloads/XMCDA-2.2.1.xsd,http://www.decision-deck.org/xmcda/_downloads/XMCDA-2.2.2.xsd,g' "$f";   
    sed -i -e 's,http://www.decision-deck.org/2013/XMCDA-3.0.0,http://www.decision-deck.org/2016/XMCDA-3.0.2,g' "$f";
    sed -i -e 's,http://www.decision-deck.org/xmcda/_downloads/XMCDA-3.0.0.xsd,http://www.decision-deck.org/xmcda/_downloads/XMCDA-3.0.2.xsd,g' "$f";        
  done
