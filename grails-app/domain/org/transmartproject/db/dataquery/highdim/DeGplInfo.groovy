package org.transmartproject.db.dataquery.highdim

import org.hibernate.cfg.NotYetImplementedException
import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.PlatformMarkerType

class DeGplInfo implements Platform {

    String id
    String title
    String organism
    Date   annotationDate
    String markerTypeId
    Integer releaseNumber

    static transients = ['markerType']

    static mapping = {
        table         schema: 'deapp'

        id            column: 'platform',   generator: 'assigned'
        markerTypeId  column: 'marker_type'
        releaseNumber column: 'release_nbr'

        version      false
    }

    static constraints = {
        id             maxSize:  50

        title          nullable: true, maxSize: 500
        organism       nullable: true, maxSize: 100
        annotationDate nullable: true
        markerTypeId   nullable: true, maxSize: 100
        releaseNumber  nullable: true
    }

    @Override
    PlatformMarkerType getMarkerType() {
        PlatformMarkerType.forId(markerTypeId)
    }

    @Override
    Iterable<?> getTemplate() {
        throw new NotYetImplementedException()
    }
}
