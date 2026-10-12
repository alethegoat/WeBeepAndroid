package it.polimi.webeep.feature.course.domain.model

data class Section(val id: Int, val name: String, val summary: String?, val sectionNumber: Int, val modules: List<Module>)
