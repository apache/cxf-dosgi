<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text" version="1.0" encoding="UTF-8" indent="yes"/>
  <xsl:template match="/">
org.ops4j.pax.web.session.timeout=30
org.osgi.framework.startlevel.beginning=200

<xsl:for-each select="//bundle">
<xsl:sort select="@start-level" data-type="number" order="ascending"/>
<xsl:variable name="i" select="position() + count(//bundles/felix_deps) + 50"/>
felix.auto.start.<xsl:value-of select="$i"/>=file:dosgi_bundles/<xsl:value-of select="@name"/>
</xsl:for-each>

</xsl:template>
</xsl:transform>
