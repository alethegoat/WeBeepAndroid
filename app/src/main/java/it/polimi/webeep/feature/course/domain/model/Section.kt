package it.polimi.webeep.feature.course.domain.model

/**
 * A course section, the unit the course screen groups modules by.
 *
 * @property sectionNumber Position of the section within the course, as numbered by Moodle.
 */
data class Section(val id: Int, val name: String, val summary: String?, val sectionNumber: Int, val modules: List<Module>)
