FROM postgres:15

# 명시적으로 권한 조정
RUN mkdir -p /var/lib/postgresql/data && \
    chown -R postgres:postgres /var/lib/postgresql/data

USER postgres

ENV PGDATA=/pgdata

CMD ["postgres"]
