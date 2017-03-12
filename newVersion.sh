DIR="*/tests/*.v2"

  # use "-name '*.java'" instead of "-type f" if you want to match java source files only

  find "${DIR}" -name '*.xml' -print | while read f; do
  #  sed -i -e 's,<ns2:XMCDA xmlns:ns2="http://www.decision-deck.org/2016/XMCDA-2.2.2">,<ns2:XMCDA xmlns:ns2="http://www.decision-deck.org/2012/XMCDA-2.2.1">,g' "$f";
  sed -i -e 's,<ns2:,<xmcda:,g'"$f";
  done

