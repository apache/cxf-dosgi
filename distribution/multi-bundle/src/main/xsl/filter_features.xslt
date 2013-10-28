<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" version="1.0" encoding="UTF-8"
        indent="yes" />

    <!-- Filter out undesired bundles -->
    <xsl:template match="bundle[@artifactId='cxf-karaf-commands']"></xsl:template>

    <!-- Copy the rest unachanged -->
    <xsl:template match="@* | node()">
        <xsl:copy>
          <xsl:apply-templates select="@* | node()" />
        </xsl:copy>
    </xsl:template>

</xsl:transform>
