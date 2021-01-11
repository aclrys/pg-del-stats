package org.prebid.pg.delstats.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import java.io.Serializable;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@IdClass(SystemState.IdClass.class)
public class SystemState {

    @Id
    String tag;

    String val;

    Timestamp updatedAt;

    @Data
    static class IdClass implements Serializable {
        String tag;
    }
}
