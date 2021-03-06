<!-- attach attributes to element -->

<stylesheet
   xmlns="http://www.w3.org/1999/XSL/Transform"
   xmlns:xt="http://www.jclark.com/xt"
   version="1.0">

<template match="/">
<element name="schema">
<copy-of select="schema/simpleType"/>
<copy-of select="schema/group"/>
<copy-of select="schema/attributeGroup"/>
<for-each select="schema/element">
<element name="element">
<choose><when test="count(*) > 0">
<copy-of select="@*"/>
<copy-of select="annotation"/>
<apply-templates select="complexType"><with-param name="of" select="@name"/></apply-templates> <!-- complexContent -->
</when><otherwise>
<for-each select="@*"><if test="name(.) != 'type'"><copy-of select="."/></if></for-each>
<call-template name="complexType"><with-param name="of" select="@name"/></call-template> <!-- simpleContent -->
</otherwise></choose>
</element>
</for-each>
</element>
</template>

<template match="*"/>

<template match="element|group|attribute|attributeGroup">
<copy-of select="."/>
</template>

<!-- simplification possible? -->
<template match="choice|sequence">
<choose><when test="count(*) = 1 and (.. = choice or .. = sequence or *[1] = group) and count(@minOccurs|@maxOccurs) = 0"> <!-- redundant choice/sequence -->
<copy-of select="*"/>
</when><otherwise>
<element name="{name(.)}">
<copy-of select="@*"/>
<apply-templates select="element|group|choice|sequence"/>
</element>
</otherwise></choose>
</template>

<template match="complexType">
<element name="complexType">
<!-- move mixed attribute -->
<variable name="ref" select="*/group/@ref"/>
<if test="count(*/@_dtd2xs_mixed_|/schema/group[@name=$ref]/*/@_dtd2xs_mixed_) > 0">
<attribute name="mixed">true</attribute>
</if>
<!-- attach attributes -->
<apply-templates select="*"/>
<param name="of"/>
<copy-of select="/schema/_dtd2xs_attributes_[@of=$of]/*"/>
</element>
</template>

<template name="complexType">
<param name="of"/>
<variable name="attributes" select="/schema/_dtd2xs_attributes_[@of=$of]/*"/>
<if test="count($attributes) > 0">
<element name="complexType">
<element name="simpleContent">
<element name="extension">
<attribute name="base"><value-of select="@type"/></attribute>
<!-- attach attributes -->
<copy-of select="$attributes"/>
</element>
</element>
</element>
</if>
</template>

</stylesheet>

