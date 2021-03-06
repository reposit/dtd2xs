<!-- attach attributes to element -->

<stylesheet
   xmlns="http://www.w3.org/1999/XSL/Transform"
   xmlns:xt="http://www.jclark.com/xt"
   version="1.0">

<template match="/">
<element name="schema">
<copy-of select="schema/simpleType"/>
<apply-templates select="schema/group"/>
<copy-of select="schema/attributeGroup"/>
<for-each select="schema/element">
<element name="element">
<choose><when test="count(*) > 0">
<copy-of select="@*"/>
<copy-of select="annotation"/>
<apply-templates select="complexType"><with-param name="of" select="@name"/></apply-templates> <!-- complexContent -->
</when><otherwise>
<copy-of select="@*[name(.) != 'type']"/>
<call-template name="complexTypeSimpleContent"><with-param name="of" select="@name"/></call-template> <!-- simpleContent -->
</otherwise></choose>
</element>
</for-each>
</element>
</template>

<template match="*"/>

<template match="element|group">
<element name="{name(.)}">
<copy-of select="@*[name(.) != '_dtd2xs_mixed_']"/>
<apply-templates select="*"/>
</element>
</template>

<template match="attribute|attributeGroup">
<copy-of select="."/>
</template>

<!-- simplification possible? -->
<template match="choice|sequence">
<choose><when test="count(*) = 1 and contains('1', @minOccurs) and contains('1', @maxOccurs) and (contains('choice|sequence', name(..)) or contains('group|choice|sequence', name(*[1])))"> <!-- redundant choice/sequence -->
<apply-templates select="element|group|choice|sequence"/>
</when><otherwise>
<element name="{name(.)}">
<copy-of select="@*[name(.) != '_dtd2xs_mixed_']"/>
<apply-templates select="element|group|choice|sequence"/>
</element>
</otherwise></choose>
</template>

<template match="complexType">
<param name="of"/>
<choose><when test="count(_dtd2xs_any_) > 0">
<call-template name="anyType"><with-param name="of" select="$of"/></call-template>
</when><otherwise>
<element name="complexType">
<!-- move mixed attribute -->
<variable name="ref" select="*/group/@ref"/>
<if test="count(*/@_dtd2xs_mixed_|/schema/group[@name=$ref]/*/@_dtd2xs_mixed_) > 0">
<attribute name="mixed">true</attribute>
</if>
<!-- attach attributes -->
<apply-templates select="*"/>
<copy-of select="/schema/_dtd2xs_attributes_[@of=$of]/*"/>
</element>
</otherwise></choose>
</template>

<template name="complexTypeSimpleContent">
<param name="of"/>
<variable name="attributes" select="/schema/_dtd2xs_attributes_[@of=$of]/*"/>
<choose><when test="count($attributes) > 0"> <!-- complexType -->
<element name="complexType">
<element name="simpleContent">
<element name="extension">
<attribute name="base"><value-of select="@type"/></attribute>
<!-- attach attributes -->
<copy-of select="$attributes"/>
</element>
</element>
</element>
</when><otherwise> <!-- simpleType -->
<copy-of select="@type"/>
</otherwise></choose>
</template>

<template name="anyType">
<param name="of"/>
<variable name="attributes" select="/schema/_dtd2xs_attributes_[@of=$of]/*"/>
<if test="count($attributes) > 0">
<element name="complexType">
<element name="complexContent">
<element name="restriction">
<attribute name="base">anyType</attribute>
<!-- attach attributes -->
<copy-of select="$attributes"/>
</element>
</element>
</element>
</if>
</template>

</stylesheet>

